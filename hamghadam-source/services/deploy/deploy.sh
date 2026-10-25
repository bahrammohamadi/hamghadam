#!/usr/bin/env bash
# استقرار کامل سرور اختیاری هم‌قدم روی Supabase + Vercel — فقط curl و python3.
#
# چرا این اسکریپت: پنل و CLI هر دو کار می‌کنند، ولی این مسیر (۱) بدون نصب چیزی،
# (۲) بدون گذاشتن راز در تاریخچهٔ شل، و (۳) قابل آزمون با سرور جعلی است.
#
# استفاده (رازها فقط از محیط خوانده می‌شوند، هرگز از آرگومان):
#   export SUPABASE_PAT=...            # توکن دسترسی سوپابیس (sbp_...)
#   export SUPABASE_PROJECT_REF=...    # ref پروژه، از URL داشبورد
#   export SUPABASE_SERVICE_ROLE_KEY=...
#   export VERCEL_TOKEN=...            # توکن ورسل (vcp_...)
#   export AI_API_KEY=...  AI_MODEL=gpt-4o-mini
#   bash hamghadam-source/services/deploy/deploy.sh
#
# برای آزمون یا استقرار در محیط دیگر، آدرس دو API را می‌توان جایگزین کرد:
#   SUPABASE_API=... VERCEL_API=... HEALTH_BASE=...
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$HERE/../api"
SQL_FILE="$HERE/../supabase/migrations/0001_init.sql"
UTIL="$HERE/jsonutil.py"
PROJECT_NAME="${PROJECT_NAME:-hamghadam-api}"

SUPABASE_API="${SUPABASE_API:-https://api.supabase.com}"
VERCEL_API="${VERCEL_API:-https://api.vercel.com}"
HEALTH_BASE="${HEALTH_BASE:-}"
SKIP_HEALTH="${SKIP_HEALTH:-0}"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
LOCAL_ENV="$HERE/.env.local"

bold() { printf '\n\033[1m== %s\033[0m\n' "$*"; }
note() { printf '   %s\n' "$*"; }
ok()   { printf '   \033[32m✔\033[0m %s\n' "$*"; }
warn() { printf '   \033[33m!\033[0m %s\n' "$*"; }
die()  { printf '\n\033[31m✖ %s\033[0m\n' "$*" >&2; exit 1; }

command -v curl >/dev/null 2>&1 || die "curl نصب نیست"
command -v python3 >/dev/null 2>&1 || die "python3 نصب نیست"

HTTP_CODE="000"
AUTH=""

# http <METHOD> <URL> [body-file]  →  کد وضعیت در HTTP_CODE، بدنه در $TMP/body
http() {
    local method="$1" url="$2" body="${3:-}"
    local -a args=(-sS -m 60 -o "$TMP/body" -w '%{http_code}' -X "$method" "$url"
                   -H 'Accept: application/json')
    [ -n "$AUTH" ] && args+=(-H "Authorization: Bearer $AUTH")
    if [ -n "$body" ]; then
        args+=(-H 'Content-Type: application/json' --data-binary "@$body")
    fi
    HTTP_CODE="$(curl "${args[@]}" 2>"$TMP/curl.err")" || HTTP_CODE="000"
    if [ "$HTTP_CODE" = "000" ]; then
        cat "$TMP/curl.err" >&2 2>/dev/null || true
    fi
}

server_error() {
    python3 "$UTIL" error "$TMP/body" 2>/dev/null || true
}

# expect <کدهای مجاز> <توضیح>
expect() {
    local allowed=" $1 " what="$2"
    case "$allowed" in
        *" $HTTP_CODE "*) ok "$what (HTTP $HTTP_CODE)"; return 0 ;;
    esac
    local message
    message="$(server_error)"
    die "$what ناموفق بود (HTTP $HTTP_CODE)${message:+ — $message}"
}

# ---------------------------------------------------------------- Supabase
deploy_supabase() {
    bold "Supabase — اعتبارسنجی دسترسی"
    [ -n "${SUPABASE_PAT:-}" ] || die "متغیر SUPABASE_PAT تنظیم نشده است"
    [ -n "${SUPABASE_PROJECT_REF:-}" ] || die "متغیر SUPABASE_PROJECT_REF تنظیم نشده است"

    AUTH="$SUPABASE_PAT"
    http GET "$SUPABASE_API/v1/projects"
    expect "200" "دریافت فهرست پروژه‌ها"

    local info
    info="$(python3 "$UTIL" project "$TMP/body" "$SUPABASE_PROJECT_REF" 2>/dev/null)" \
        || die "پروژهٔ «$SUPABASE_PROJECT_REF» در حساب سوپابیس نیست یا توکن دسترسی ندارد"
    note "پروژه: $(printf '%s' "$info" | cut -f2) ($(printf '%s' "$info" | cut -f1))"

    bold "Supabase — اعمال اسکیما و RLS"
    python3 "$UTIL" querybody "$SQL_FILE" > "$TMP/query.json"
    http POST "$SUPABASE_API/v1/projects/$SUPABASE_PROJECT_REF/database/query" "$TMP/query.json"
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
        ok "کل اسکیما در یک رفت‌وبرگشت اعمال شد"
    else
        warn "ارسال یک‌جا نشد (HTTP $HTTP_CODE): $(server_error)"
        note "تلاش دوباره به‌صورت دستور‌به‌دستور…"
        local count=0 line
        python3 "$UTIL" statements "$SQL_FILE" > "$TMP/statements.jsonl" \
            || die "تقسیم SQL ناموفق بود"
        while IFS= read -r line; do
            count=$((count + 1))
            printf '{"query":%s}' "$line" > "$TMP/statement.json"
            http POST "$SUPABASE_API/v1/projects/$SUPABASE_PROJECT_REF/database/query" "$TMP/statement.json"
            case "$HTTP_CODE" in
                200|201) ;;
                *) die "دستور شمارهٔ $count اعمال نشد (HTTP $HTTP_CODE) — $(server_error)" ;;
            esac
        done < "$TMP/statements.jsonl"
        ok "همهٔ $count دستور با موفقیت اعمال شد"
    fi

    bold "Supabase — کلیدهای عمومی"
    http GET "$SUPABASE_API/v1/projects/$SUPABASE_PROJECT_REF/api-keys"
    expect "200" "دریافت کلیدهای پروژه"
    SUPABASE_ANON_KEY="$(python3 "$UTIL" apikey "$TMP/body")" \
        || die "کلید anon در پاسخ نبود (پروژه در حال ساخت است؟ کمی بعد دوباره اجرا کنید)"
    SUPABASE_URL="https://$SUPABASE_PROJECT_REF.supabase.co"
    ok "آدرس پروژه: $SUPABASE_URL"
    ok "کلید عمومی anon دریافت شد (${#SUPABASE_ANON_KEY} نویسه)"
}

# ------------------------------------------------------------------ Vercel
deploy_vercel() {
    bold "Vercel — پروژه"
    [ -n "${VERCEL_TOKEN:-}" ] || die "متغیر VERCEL_TOKEN تنظیم نشده است"
    AUTH="$VERCEL_TOKEN"

    printf '{"name":"%s","framework":null}' "$PROJECT_NAME" > "$TMP/project.json"
    http POST "$VERCEL_API/v11/projects" "$TMP/project.json"
    case "$HTTP_CODE" in
        200|201) ok "پروژهٔ $PROJECT_NAME ساخته شد" ;;
        400|409)
            http GET "$VERCEL_API/v9/projects/$PROJECT_NAME"
            expect "200" "پروژهٔ موجود پیدا شد"
            ;;
        *) die "ساخت پروژه ناموفق بود (HTTP $HTTP_CODE) — $(server_error)" ;;
    esac

    bold "Vercel — متغیرهای محیطی (فقط سمت سرور)"
    [ -n "${SUPABASE_ANON_KEY:-}" ] || die "کلید anon در دسترس نیست"
    [ -n "${SUPABASE_SERVICE_ROLE_KEY:-}" ] || die "متغیر SUPABASE_SERVICE_ROLE_KEY تنظیم نشده است"

    local pairs=(
        "SUPABASE_URL=$SUPABASE_URL"
        "SUPABASE_ANON_KEY=$SUPABASE_ANON_KEY"
        "SUPABASE_SERVICE_ROLE_KEY=$SUPABASE_SERVICE_ROLE_KEY"
        "AI_BASE_URL=${AI_BASE_URL:-https://api.openai.com/v1}"
        "AI_MODEL=${AI_MODEL:-gpt-4o-mini}"
        "AI_API_KEY=${AI_API_KEY:-}"
        "ALLOWED_ORIGINS=${ALLOWED_ORIGINS:-}"
    )
    local pair key value env_id
    for pair in "${pairs[@]}"; do
        key="${pair%%=*}"
        value="${pair#*=}"
        if [ -z "$value" ] && [ "$key" = "AI_API_KEY" ]; then
            warn "$key خالی است — دستیار در این استقرار خاموش می‌ماند (health: agent=false)"
        fi
        python3 - "$TMP/env.json" "$key" "$value" <<'PY'
import json, sys
path, key, value = sys.argv[1], sys.argv[2], sys.argv[3]
json.dump({"key": key, "value": value, "type": "encrypted",
           "target": ["production", "preview"]}, open(path, "w", encoding="utf-8"))
PY
        http POST "$VERCEL_API/v10/projects/$PROJECT_NAME/env" "$TMP/env.json"
        case "$HTTP_CODE" in
            200|201) ok "$key ثبت شد" ;;
            400|409)
                http GET "$VERCEL_API/v9/projects/$PROJECT_NAME/env"
                expect "200" "فهرست متغیرهای موجود"
                env_id="$(python3 "$UTIL" envid "$TMP/body" "$key" 2>/dev/null)" \
                    || die "متغیر $key وجود دارد ولی شناسه‌اش پیدا نشد"
                printf '{"value":"%s"}' "$(python3 -c 'import json,sys;print(json.dumps(sys.argv[1])[1:-1])' "$value")" \
                    > "$TMP/env-patch.json"
                http PATCH "$VERCEL_API/v9/projects/$PROJECT_NAME/env/$env_id" "$TMP/env-patch.json"
                expect "200 201" "به‌روزرسانی $key"
                ;;
            *) die "ثبت $key ناموفق بود (HTTP $HTTP_CODE) — $(server_error)" ;;
        esac
    done

    bold "Vercel — انتشار"
    python3 "$UTIL" deploybody "$API_DIR" "$PROJECT_NAME" > "$TMP/deploy.json" \
        || die "ساخت بدنهٔ استقرار ناموفق بود"
    note "حجم بستهٔ استقرار: $(wc -c < "$TMP/deploy.json" | tr -d ' ') بایت"
    http POST "$VERCEL_API/v13/deployments" "$TMP/deploy.json"
    expect "200 201" "ایجاد استقرار"
    local deployment_id
    deployment_id="$(python3 "$UTIL" field "$TMP/body" id)" || die "شناسهٔ استقرار در پاسخ نبود"

    local attempt state
    for attempt in $(seq 1 40); do
        sleep 5
        http GET "$VERCEL_API/v13/deployments/$deployment_id"
        [ "$HTTP_CODE" = "200" ] || die "وضعیت استقرار خوانده نشد (HTTP $HTTP_CODE)"
        state="$(python3 "$UTIL" field "$TMP/body" readyState)"
        case "$state" in
            READY) ok "استقرار آماده است (تلاش $attempt)"; break ;;
            ERROR|CANCELED) die "استقرار با وضعیت $state پایان یافت" ;;
            *) note "وضعیت: $state — بررسی دوباره تا ۵ ثانیه دیگر" ;;
        esac
        [ "$attempt" = "40" ] && die "استقرار در ۲۰۰ ثانیه آماده نشد"
    done

    DEPLOY_URL="https://$(python3 "$UTIL" field "$TMP/body" url)"
    ok "آدرس: $DEPLOY_URL"
}

# ------------------------------------------------------------------ گزارش
write_local_env() {
    umask 077
    {
        printf '# ساختهٔ deploy.sh — این فایل در .gitignore است و نباید منتشر شود.\n'
        printf 'SUPABASE_URL=%s\n' "${SUPABASE_URL:-}"
        printf 'SUPABASE_ANON_KEY=%s\n' "${SUPABASE_ANON_KEY:-}"
        printf 'API_BASE_URL=%s\n' "${DEPLOY_URL:-}"
    } > "$LOCAL_ENV"
}

health_check() {
    [ "$SKIP_HEALTH" = "1" ] && { warn "آزمون سلامت رد شد (SKIP_HEALTH=1)"; return 0; }
    local base="${HEALTH_BASE:-${DEPLOY_URL:-}}"
    [ -n "$base" ] || { warn "آدرسی برای آزمون سلامت نیست"; return 0; }
    bold "آزمون سلامت"
    http GET "$base/api/health"
    expect "200" "GET $base/api/health"
    cat "$TMP/body" | python3 -c 'import json,sys;d=json.load(sys.stdin);print("   پاسخ:",json.dumps(d,ensure_ascii=False))' \
        || warn "پاسخ JSON نبود"
}

main() {
    bold "هم‌قدم — استقرار سرور اختیاری"
    [ -f "$SQL_FILE" ] || die "فایل اسکیما پیدا نشد: $SQL_FILE"
    [ -f "$API_DIR/api/health.js" ] || die "فایل‌های API پیدا نشد: $API_DIR"

    deploy_supabase
    deploy_vercel
    write_local_env
    health_check

    bold "پایان"
    ok "متغیرهای لازم برای اپ در $LOCAL_ENV نوشته شد (بدون commit)"
    note "کلید AI فقط در Vercel است؛ در اپ هیچ‌جا نوشته نمی‌شود."
}

main "$@"
