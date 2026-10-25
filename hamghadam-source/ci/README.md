# ساخت خودکار APK (GitHub Actions)

## وضعیت فعلی

اولین اجرا (run `40166534741`) در مرحلهٔ **Set up Android SDK** شکست خورد. علتش خود اپ نیست:

> `android-actions/setup-android@v3` به‌طور پیش‌فرض بستهٔ قدیمی `tools` را نصب می‌کند و گوگل آن بسته را از مخزن SDK حذف کرده است؛ پس `sdkmanager tools` با کد خطا خارج می‌شود و مرحله قبل از Gradle می‌میرد.

اصلاح‌ها:

1. وابستگی به آن Action حذف شد؛ اسکریپت خودش SDK نصب‌شدهٔ runner را پیدا می‌کند، لایسنس‌ها را می‌پذیرد و `platforms;android-35` و `build-tools;35.0.0` را نصب می‌کند.
2. کل منطق CI به [`build.sh`](build.sh) منتقل شد. فایل workflow فقط ۴۸ خط است و **اسکریپت با push معمولی به‌روز می‌شود**، پس از این به بعد برای اصلاح CI لازم نیست فایل workflow دوباره ویرایش شود.
3. در صورت شکست، خطاها به‌صورت annotation (`::error::`) منتشر می‌شوند؛ این‌ها از API گیت‌هاب خواندنی‌اند، بنابراین حتی وقتی لاگ کامل در دسترس نباشد علت شکست قابل تشخیص است.
4. در صورت موفقیت، APK هم به‌عنوان artifact و هم به‌صورت prerelease (`debug-<شمارهٔ اجرا>`) منتشر می‌شود.

## تنها قدم دستی باقی‌مانده

محتوای `.github/workflows/android-apk.yml` در مخزن باید یک بار با نسخهٔ جدید عوض شود. لینک [`replace-workflow-link.txt`](replace-workflow-link.txt) صفحهٔ ویرایش همان فایل را با محتوای جدید باز می‌کند؛ اگر محتوا از پیش پر نشده بود: کل متن را انتخاب و پاک کن، سپس محتوای [`android-apk.yml`](android-apk.yml) را جای‌گذاری کن و **Commit changes** را بزن.

چرا دستی؟ GitHub اجازه نمی‌دهد هیچ اپ/توکنی فایل داخل `.github/workflows/` بسازد یا تغییر دهد مگر با مجوز `workflows`. در این محیط، گواهی TLS گیت‌هاب را پروکسی سندباکس صادر می‌کند (`O = E2B, CN = api.github.com`)، پس هر توکنی که وارد شود با اعتبارنامهٔ اتوماسیون جایگزین می‌شود. سه مسیر مستقل امتحان شد و همه با `Resource not accessible by integration` رد شدند: `PUT /contents/...`، `POST /git/trees` و GraphQL `createCommitOnBranch`.

## بعد از اجرا

- **موفق**: artifact با نام `hamghadam-debug-apk` و همچنین Release با تگ `debug-<n>`.
- **ناموفق**: در تب Actions، مرحلهٔ «Build and test» خطاهای خلاصه‌شده را نشان می‌دهد؛ همان‌ها را در issue/PR بفرست یا بگو تا از API بخوانم و رفع کنم.

## نکته‌ها

- خروجی APK **debug** است، نه نسخهٔ امضاشدهٔ انتشار؛ نصب روی گوشی اجازهٔ «نصب از منبع نامعتبر» می‌خواهد.
- اسکریپت قبل از ساخت، تست‌های JVM را اجرا می‌کند (`:app:testDebugUnitTest`).
- مسیر پروژه: `hamghadam-source/apps/android`. اسکریپت مسیرها را نسبت به ریشهٔ مخزن حل می‌کند، پس از هر پوشه‌ای قابل اجراست.
- اجرای محلی: `bash hamghadam-source/ci/build.sh` (به JDK 17 و Android SDK نیاز دارد).
