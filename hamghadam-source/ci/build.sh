#!/usr/bin/env bash
# ساخت و تست APK اندروید هم‌قدم — منطق کامل CI اینجاست، نه در فایل workflow.
# چرا: فایل‌های .github/workflows/ با توکن این مخزن قابل ویرایش نیستند، ولی
# این اسکریپت با push معمولی به‌روز می‌شود و روی هر اجرا از مخزن خوانده می‌شود.
#
# استفاده محلی:  bash hamghadam-source/ci/build.sh
# در CI:         workflow فقط checkout + JDK + Gradle می‌کند و همین فایل را اجرا می‌کند.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT" || { echo "::error::ریشهٔ مخزن پیدا نشد"; exit 1; }
PROJECT_DIR="${PROJECT_DIR:-hamghadam-source/apps/android}"
WORK_DIR="${RUNNER_TEMP:-/tmp}/hamghadam-ci"
LOG_FILE="$WORK_DIR/gradle.log"
SDK_LOG="$WORK_DIR/sdk.log"

mkdir -p "$WORK_DIR"
: > "$LOG_FILE"
: > "$SDK_LOG"

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }
notice() { printf '::notice::%s\n' "$1"; }

# ---------------------------------------------------------------- Android SDK
find_sdk() {
    local c
    for c in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" \
             /usr/local/lib/android/sdk "${HOME:-/root}/Android/Sdk" \
             /opt/android-sdk /usr/lib/android-sdk; do
        if [ -n "$c" ] && [ -d "$c" ]; then
            if [ -d "$c/platforms" ] || [ -d "$c/cmdline-tools" ] || [ -d "$c/tools" ]; then
                printf '%s\n' "$c"; return 0
            fi
        fi
    done
    return 1
}

find_sdkmanager() {
    local sdk="$1" c
    for c in "$sdk/cmdline-tools/latest/bin/sdkmanager" \
             "$sdk/tools/bin/sdkmanager"; do
        [ -x "$c" ] && { printf '%s\n' "$c"; return 0; }
    done
    c=$(find "$sdk/cmdline-tools" -maxdepth 3 -type f -name sdkmanager 2>/dev/null | head -1)
    [ -n "$c" ] && [ -x "$c" ] && { printf '%s\n' "$c"; return 0; }
    command -v sdkmanager 2>/dev/null && return 0
    return 1
}

# ---------------------------------------------------------------- error report
# هر خط خطا را به annotation تبدیل می‌کند تا حتی وقتی لاگ کامل در دسترس نیست
# (یا کاربر جایی دور است) بشود علت شکست را از API گیت‌هاب خواند.
report() {
    local title="$1"
    say "$title"
    if [ ! -s "$LOG_FILE" ]; then
        echo "::error::$title (بدون لاگ)"
        return
    fi

    local errs
    errs=$(grep -nE '^e: |^error:|^FAILURE:|What went wrong|Execution failed|Could not (resolve|find|determine|download|create|open)|Caused by:|Unresolved reference|Cannot access|None of the following|error:' "$LOG_FILE" 2>/dev/null | head -60)
    [ -z "$errs" ] && errs=$(tail -n 40 "$LOG_FILE")

    local line n=0
    while IFS= read -r line; do
        n=$((n + 1))
        [ "$n" -gt 40 ] && break
        line="${line:0:900}"
        printf '%s\n' "$line"
        printf '::error::%s\n' "$line"
    done <<< "$errs"

    { echo "### ❌ $title"; echo '```'; tail -n 80 "$LOG_FILE"; echo '```'; } \
        >> "${GITHUB_STEP_SUMMARY:-/dev/null}" 2>/dev/null
}

# پیام کوتاه بدون لاگ (خطاهای زیرساختی)
die() {
    say "$1"
    printf '::error::%s\n' "$1"
    exit 1
}

# خطای مرحلهٔ ساخت: لاگ هم گزارش می‌شود
die_with_log() {
    report "$1"
    exit 1
}

# ---------------------------------------------------------------- toolchain
say "محیط"
uname -a || true
java -version > "$WORK_DIR/java.txt" 2>&1 || true
head -3 "$WORK_DIR/java.txt" 2>/dev/null || true

if ! command -v gradle >/dev/null 2>&1; then
    say "Gradle روی PATH نیست؛ دانلود Gradle 8.9"
    curl -fsSL -o "$WORK_DIR/gradle.zip" https://services.gradle.org/distributions/gradle-8.9-bin.zip || die "دانلود Gradle ناموفق بود"
    (command -v unzip >/dev/null 2>&1 && unzip -q "$WORK_DIR/gradle.zip" -d "$WORK_DIR") || die "باز کردن بستهٔ Gradle ناموفق بود"
    export PATH="$WORK_DIR/gradle-8.9/bin:$PATH"
fi
gradle --version > "$WORK_DIR/gradle-version.txt" 2>&1 || die "Gradle اجرا نشد"
head -8 "$WORK_DIR/gradle-version.txt" || true

say "Android SDK"
SDK=$(find_sdk) || die "Android SDK روی این runner پیدا نشد"
export ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK"
export PATH="$SDK/platform-tools:$PATH"
echo "ANDROID_HOME=$ANDROID_HOME"

SM=$(find_sdkmanager "$SDK") || echo "::warning::sdkmanager پیدا نشد؛ به SDK نصب‌شدهٔ runner تکیه می‌کنیم"

if [ -n "${SM:-}" ]; then
    yes | "$SM" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
    if ! "$SM" --sdk_root="$SDK" --install "platform-tools" "platforms;android-35" "build-tools;35.0.0" \
            >>"$SDK_LOG" 2>&1; then
        echo "::warning::نصب بسته‌های Android SDK گزارش خطا داد؛ اگر بسته‌ها از قبل نصب باشند مشکلی نیست"
        tail -n 20 "$SDK_LOG" || true
    fi
    "$SM" --sdk_root="$SDK" --list_installed 2>/dev/null | sed -n '1,25p' || true
fi

for need in "platforms/android-35" "build-tools/35.0.0"; do
    [ -d "$SDK/$need" ] || die "بستهٔ SDK لازم موجود نیست: $need"
done

# ---------------------------------------------------------------- project
say "گرادی/اندروید پروژه"
cd "$PROJECT_DIR" || die "مسیر پروژهٔ اندروید پیدا نشد: $PROJECT_DIR"
printf 'sdk.dir=%s\n' "$SDK" > local.properties

GRADLE_ARGS=(--no-daemon --stacktrace --console=plain -Dorg.gradle.jvmargs=-Xmx3g)

say "تست‌های JVM"
if ! gradle "${GRADLE_ARGS[@]}" :app:testDebugUnitTest 2>&1 | tee -a "$LOG_FILE"; then
    die_with_log "تست‌های JVM شکست خوردند (مرحله: testDebugUnitTest)"
fi

say "ساخت APK دیباگ"
if ! gradle "${GRADLE_ARGS[@]}" :app:assembleDebug 2>&1 | tee -a "$LOG_FILE"; then
    die_with_log "ساخت APK شکست خورد (مرحله: assembleDebug)"
fi

APK="app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] || die "فایل APK ساخته نشد: $APK"
ls -lh "$APK" || true
APK_SIZE=$(wc -c < "$APK" | tr -d ' ')
say "بررسی محتوای APK"
[ "$APK_SIZE" -gt 1000000 ] || die "حجم APK غیرعادی کم است: $APK_SIZE بایت"

# ساختار ZIP و فایل‌های الزامی
if command -v unzip >/dev/null 2>&1; then
    unzip -t "$APK" > "$WORK_DIR/apk-test.log" 2>&1 || die_with_log "APK یک بستهٔ ZIP سالم نیست"
    for f in AndroidManifest.xml classes.dex resources.arsc; do
        unzip -l "$APK" 2>/dev/null | grep -q " $f" || die "فایل $f داخل APK نیست"
    done
    notice "APK ساختار سالم دارد (zip ok، manifest + dex + resources)"
fi

# مشخصات بسته با aapt2/aapt
AAPT=""
for c in "$SDK/build-tools/35.0.0/aapt2" "$SDK/build-tools/35.0.0/aapt" "${SM:+$(dirname "$SM")/aapt2}"; do
    [ -n "$c" ] && [ -x "$c" ] && { AAPT="$c"; break; }
done
if [ -n "$AAPT" ]; then
    # خروجی کامل در فایل ذخیره می‌شود: badging برای هر زبان یک application-label دارد
    # و برش‌دادنش باعث می‌شد launchable-activity از انتها گم شود.
    "$AAPT" dump badging "$APK" > "$WORK_DIR/badging.txt" 2>"$WORK_DIR/badging.err" || true
    BADGING_FILE="$WORK_DIR/badging.txt"
    grep -E "^(package|sdkVersion|minSdkVersion|targetSdkVersion|application-label|launchable-activity):?" \
        "$BADGING_FILE" | head -12 || true
    PKG=$(sed -n "s/^package: name='\\([^']*\\)'.*/\1/p" "$BADGING_FILE" | head -1)
    VC=$(sed -n "s/.*versionCode='\\([^']*\\)'.*/\1/p" "$BADGING_FILE" | head -1)
    MINSDK=$(sed -n "s/^\\(min\\)\\{0,1\\}sdkVersion:'\\([^']*\\)'.*/\\2/p" "$BADGING_FILE" | head -1)
    [ -n "$MINSDK" ] || MINSDK=$(awk -F"'" '/sdkVersion/ {print $2; exit}' "$BADGING_FILE" 2>/dev/null || true)
    [ -n "$MINSDK" ] || MINSDK=$(grep -m1 -oE "sdkVersion[^0-9]*[0-9]+" "$BADGING_FILE" 2>/dev/null | grep -oE "[0-9]+$" || true)
    LAUNCH=$(grep -c "^launchable-activity" "$BADGING_FILE" || true)
    notice "APK: package=${PKG:-?} versionCode=${VC:-?} launcher=${LAUNCH:-0} size=${APK_SIZE}B${MINSDK:+ minSdk=$MINSDK}"
    [ "${PKG:-}" = "ir.hamghadam.app" ] || die "شناسهٔ بستهٔ APK غیرمنتظره است: ${PKG:-خالی}"
    [ "${LAUNCH:-0}" -ge 1 ] || die "APK هیچ activity قابل اجرا (launcher) ندارد"
else
    echo "::warning::aapt2 پیدا نشد؛ بررسی مشخصات بسته انجام نشد"
fi

# ---------------------------------------------------------------- release (best effort)
if [ -n "${GH_TOKEN:-}" ] && command -v gh >/dev/null 2>&1 \
        && [ "${GITHUB_EVENT_NAME:-}" != "pull_request" ]; then
    say "انتشار APK در release غلتان"
    TAG="${RELEASE_TAG:-latest-debug}"
    SHA7=$(printf '%s' "${GITHUB_SHA:-unknown}" | cut -c1-7)
    NOTES=$(cat <<TXT
آخرین APK دیباگ ساخته‌شده از شاخهٔ \`${GITHUB_REF_NAME:-?}\` (commit ${SHA7}، اجرای #${GITHUB_RUN_NUMBER:-0}).

- شناسهٔ بسته: \`ir.hamghadam.app\` — نسخهٔ 0.3.0 (versionCode 3)
- حداقل اندروید: 8.0 (API 26)
- حجم: \`${APK_SIZE}\` بایت
- امضا: debug (برای تست، بدون امضای انتشار)

نصب: فایل \`app-debug.apk\` را دانلود کن، «نصب از منبع نامعتبر» را برای مرورگر فعال کن و نصب کن.
این release با هر بیلد موفق به‌روزرسانی می‌شود.
TXT
    )
    if gh release view "$TAG" >/dev/null 2>&1; then
        gh release upload "$TAG" "$APK#app-debug.apk" --clobber >>"$SDK_LOG" 2>&1 \
            && gh release edit "$TAG" --title "هم‌قدم — آخرین APK دیباگ" --notes "$NOTES" --prerelease >>"$SDK_LOG" 2>&1 \
            || echo "::warning::به‌روزرسانی release ممکن نشد (مجوز contents: write؟)"
    else
        gh release create "$TAG" "$APK#app-debug.apk" \
            --title "هم‌قدم — آخرین APK دیباگ" --notes "$NOTES" --prerelease >>"$SDK_LOG" 2>&1 \
            || echo "::warning::ساخت release ممکن نشد (مجوز contents: write؟)"
    fi
    echo "::notice::release: ${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY:-}/releases/tag/$TAG"
fi

say "تمام"
{ echo "### ✅ APK ساخته شد"; echo "- تست‌ها: سبز"; echo "- حجم: ${APK_SIZE} بایت"; echo "- فایل: \`$APK\`"; } \
    >> "${GITHUB_STEP_SUMMARY:-/dev/null}" 2>/dev/null
exit 0
