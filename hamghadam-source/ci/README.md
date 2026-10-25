# ساخت خودکار APK (GitHub Actions) — ✅ فعال و سبز

workflow در `.github/workflows/android-apk.yml` قرار دارد و روی هر push و PR اجرا می‌شود. فقط `bash hamghadam-source/ci/build.sh` را صدا می‌زند؛ **تمام منطق در [`build.sh`](build.sh) است**، پس اصلاح‌های بعدی CI با push عادی انجام می‌شود و نیازی به ویرایش فایل workflow نیست.

## آخرین وضعیت

| مورد | نتیجه |
|---|---|
| اجرای `40166534741` | ❌ شکست در `setup-android@v3` (بستهٔ حذف‌شدهٔ `tools`) |
| اجرای `40166923073` (push) و `40166925367` (PR) | ✅ سبز، ~۳ دقیقه |
| تست‌های JVM | ✅ `:app:testDebugUnitTest` |
| APK دیباگ | ✅ `app-debug.apk` (~۹.۹MB) |
| انتشار | ✅ release غلتان `latest-debug` (با هر بیلد موفق به‌روزرسانی می‌شود) |
| Artifact | ✅ `hamghadam-debug-apk` (۳۰ روز نگهداری) |

دانلود APK: [releases/tag/latest-debug](https://github.com/bahrammohamadi/hamghadam/releases/tag/latest-debug) → `app-debug.apk`.

## چه چیزی در build.sh انجام می‌شود

1. پیدا کردن Android SDK روی runner (بدون وابستگی به `setup-android`)، پذیرش لایسنس‌ها و نصب `platforms;android-35` و `build-tools;35.0.0`؛ اگر SDK از قبل کامل باشد فقط بررسی می‌شود.
2. نوشتن `local.properties` با مسیر درست SDK.
3. `:app:testDebugUnitTest` سپس `:app:assembleDebug` (Gradle 8.9، JDK 17).
4. در صورت شکست: خطاهای Gradle به‌صورت `::error::` منتشر می‌شوند (از API گیت‌هاب خواندنی‌اند) و ۸۰ خط آخر لاگ در Step Summary می‌آید.
5. **تأیید محتوای APK پیش از انتشار:** سالم بودن ZIP، وجود `AndroidManifest.xml`، `classes.dex` و `resources.arsc`، حداقل حجم ۱MB، و خواندن `aapt2 dump badging`؛ شناسهٔ بسته باید دقیقاً `ir.hamghadam.app` باشد و APK باید حداقل یک activity قابل اجرا (launcher) داشته باشد. اگر هر کدام رد شود جاب fail می‌شود و Release ساخته نمی‌شود.
6. در صورت موفقیت: به‌روزرسانی release غلتان `latest-debug` با APK جدید (نیازمند `permissions: contents: write` که در workflow تنظیم شده). به‌جای ساخت یک release برای هر اجرا، همان یکی به‌روز می‌شود تا صفحهٔ Releases شلوغ نشود.

نمونهٔ خروجی تأیید از آخرین اجرا (annotation قابل خواندن از API):

```
[notice] APK ساختار سالم دارد (zip ok، manifest + dex + resources)
[notice] APK: package=ir.hamghadam.app versionCode=3 launcher=1 size=9931564B
```

## چرا فایل workflow دستی در مخزن قرار گرفت؟

GitHub اجازه نمی‌دهد هیچ اپ/توکنی فایل داخل `.github/workflows/` بسازد یا تغییر دهد مگر با مجوز `workflows`. در این محیط گواهی TLS گیت‌هاب را پروکسی سندباکس صادر می‌کند (`O = E2B, CN = api.github.com`)، پس هر توکنی با اعتبارنامهٔ اتوماسیون جایگزین می‌شود؛ سه مسیر مستقل امتحان شد و همه با `Resource not accessible by integration` رد شدند (`PUT /contents/...`، `POST /git/trees`، GraphQL `createCommitOnBranch`). یک بار جای‌گذاری دستی انجام شد و از این پس لازم نیست.

## اجرای محلی

```bash
bash hamghadam-source/ci/build.sh      # نیازمند JDK 17 و Android SDK 35
```

اسکریپت مسیرها را نسبت به ریشهٔ مخزن حل می‌کند، پس از هر پوشه‌ای قابل اجراست. برای اجرای محلی می‌توانی `PROJECT_DIR` را هم بدهی.

## نکته‌ها

- خروجی APK **debug** است، نه نسخهٔ امضاشدهٔ انتشار؛ نصب روی گوشی اجازهٔ «نصب از منبع نامعتبر» می‌خواهد.
- `retention-days: 30` برای artifact؛ Release محدودیت زمانی ندارد.
- برای نسخهٔ امضاشده، keystore در Secrets و یک `signingConfig` لازم است (هنوز پیاده‌سازی نشده).
