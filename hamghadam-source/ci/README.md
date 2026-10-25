# ساخت خودکار APK

فایل `ci/android-apk.yml` همان workflow آمادهٔ GitHub Actions است. برای فعال‌کردن آن فقط یک قدم دستی لازم است:

1. در صفحهٔ مخزن GitHub روی **Add file → Create new file** بزن.
2. نام فایل را دقیقاً این بگذار: `.github/workflows/android-apk.yml`
3. محتوای `ci/android-apk.yml` را داخل آن جای‌گذاری و commit کن.
4. به تب **Actions** برو، workflow با نام **Build Android APK** را انتخاب کن و **Run workflow** را بزن.
5. پس از پایان، از بخش Artifacts فایل `hamghadam-debug-apk` را دانلود کن؛ `app-debug.apk` داخل آن است.

این قدم چون توکن/اپ مورد استفاده در این شاخه اجازهٔ نوشتن در مسیر `.github/workflows/` را ندارد به‌صورت دستی مانده است. اگر App مربوط به اتوماسیون مجوز **Workflows** بگیرد، همان فایل می‌تواند مستقیم در جای خودش قرار بگیرد.

نکته‌ها:

- خروجی یک APK **debug** است، نه نسخهٔ امضاشدهٔ انتشار.
- برای نصب روی گوشی، اجازهٔ «نصب از منبع نامعتبر» لازم است.
- workflow قبل از ساخت APK تست‌های JVM را اجرا می‌کند (`:app:testDebugUnitTest`) و در صورت شکست، گزارش تست را به‌عنوان artifact می‌گذارد.
- مسیر پروژهٔ اندروید در این مخزن `hamghadam-source/apps/android` است؛ اگر ساختار پوشه‌ها را تغییر دادی، مقدار `working-directory` را هم به‌روز کن.
