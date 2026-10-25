# معماری و مدل دادهٔ اولیه

## معماری پیشنهادی
```text
Android (Compose, Room, DataStore)
  ├─ Domain/use-cases ─ repositories ─ local DB (source of truth)
  ├─ Outbox (durable ops) ─ WorkManager sync worker ─ REST/OpenAPI
  ├─ Alarm scheduling (local) / notification channels
  └─ Optional adapters: device calendar, speech, OCR, AI
API Gateway / NestJS modular monolith
  ├─ Identity & sessions / Workspaces & authorization
  ├─ Tasks / Calendar / Projects / Habits / Focus
  ├─ Sync & idempotency / Notifications / Billing / AI gateway
  ├─ PostgreSQL (tenant/workspace scoped) ├ Redis (cache/queues/rate limit)
  └─ S3-compatible object storage (signed URL; malware scan)
Admin web: separate origin + MFA + least privilege + audited operations
```

**تصمیم:** در شروع Modular Monolith از microservices کم‌هزینه‌تر و ساده‌تر برای عملیات و تراکنش است. ماژول‌ها مرز دامنه و interface مستقل دارند تا بعداً اجزای پرترافیک جدا شوند. PostgreSQL مرجع؛ Redis هرگز منبع حقیقت نیست. Docker Compose فقط توسعه/آزمون، نه طرح نهایی HA. استقرار داخل ایران پس از ارزیابی ارائه‌دهنده، دسترسی، پشتیبان خارج از سایت و الزامات قانونی.

### لایه Android
Kotlin، Compose، Material 3 با Design Tokens، Clean Architecture/MVVM، Room، DataStore، Hilt، Flow/Coroutines، WorkManager برای sync، AlarmManager فقط برای اعلان دقیق با مجوز/رفتار fallback، App Widgets. حداقل SDK و target SDK بعد از بررسی دستگاه‌های بازار و سیاست Play تعیین شود. تاریخ با `java.time.Instant`, `LocalDate`, `ZoneId`; محاسبه Jalali در adapter معتبر و با corpus آزمون؛ UI هرگز زمان محلی را با UTC-00 فرض نکند.

### Backend
NestJS + TypeScript، REST نسخه‌دار `/v1`, OpenAPI؛ PostgreSQL با FK/unique و محدودیت‌های دامنه، Redis برای صف/rate-limit، object storage با URL امضاشده. هر درخواست tenant/workspace از JWT به actor resolve شود و policy در use-case/repository اعمال شود. همهٔ دسترسی‌های ID با آزمون مالکیت. WebSocket صرفاً برای اعلان تغییر و پس از auth؛ کلاینت همچنان sync cursor را مرجع می‌گیرد.

## مدل داده (طرح منطقی اولیه)
تمام موجودیت‌های هم‌زمان‌شونده: `id UUID`, `created_at`, `updated_at`, `version BIGINT`, `deleted_at NULL` (tombstone), `workspace_id` در موارد اشتراکی. زمان‌ها UTC (`timestamptz`)، زمان‌های محلی/رویدادهای تمام‌روزه همراه `timezone` و date-only مستقل. IDها تصادفی، API پاسخ کمینه.

- `users(id, display_name, locale, timezone, week_start, created_at, deleted_at)`
- `identities(id, user_id FK, provider, subject, phone_e164_encrypted, verified_at)`؛ unique(provider, subject)، شماره canonical/HMAC برای lookup؛ OTP plaintext هرگز ذخیره نشود.
- `devices(id,user_id,platform,push_token_encrypted,last_seen_at)`, `sessions(id,user_id,refresh_token_hash,expires_at,revoked_at)`
- `workspaces(id, owner_id, type, name, plan_id)`, `roles(id,workspace_id,name,permissions JSONB)`, `workspace_members(workspace_id,user_id,role_id,status)` unique pair
- `projects(id,workspace_id,parent_id,name,status,sort_order)`, `project_members(project_id,user_id,role_id)`
- `tasks(id,workspace_id,project_id,parent_task_id,title,description,status,priority,start_at,due_at,scheduled_start,scheduled_end,estimate_minutes,timezone,completed_at,created_by,version,deleted_at)`؛ index workspace/status/due, project/order, parent.
- `task_dependencies(task_id,depends_on_task_id,type)` unique pair; check no self-link; cycle detection service.
- `task_assignees(task_id,user_id,assigned_by)` unique pair; `task_recurrences(task_id,rule_json,calendar_type,timezone,until_at,exception_dates)`؛ قاعدهٔ recurrence با RFC 5545-compatible subset و قواعد مشخص missing-day.
- `checklists(id,task_id,title,position)`, `checklist_items(id,checklist_id,text,checked_at,position)` با quota سروری و hard limit حفاظتی.
- `tags(id,workspace_id,name,color)`, `task_tags(task_id,tag_id)` unique pair
- `attachments(id,workspace_id,task_id,object_key,mime,size,sha256,scan_status,created_by)`؛ دسترسی signed URL کوتاه‌مدت.
- `comments(id,workspace_id,task_id,author_id,body,created_at)`, `reminders(id,task_id,fire_at,channel,rule_json,status)`؛ dedupe key.
- `calendar_events(id,workspace_id,task_id,title,start_at,end_at,all_day_start,all_day_end,timezone,source,external_id)`؛ exclusive-end استاندارد برای تمام‌روزه.
- `habits(id,workspace_id,name,kind,goal_rule_json,timezone,archived_at)`, `habit_logs(id,habit_id,local_date,status,note)` unique(habit_id,local_date)
- `goals(id,workspace_id,title,period_start,period_end,status)`, `milestones(id,goal_id,task_id,title,due_at,status)`
- `focus_sessions(id,user_id,task_id,started_at,ended_at,planned_minutes,actual_minutes,kind)`؛ پایان session idempotent
- `time_entries(id,task_id,user_id,started_at,ended_at,source)`؛ check ended > started
- `notifications(id,user_id,kind,payload_json,scheduled_at,sent_at,dedupe_key)` unique(user_id,dedupe_key)
- `sync_operations(id,actor_id,device_id,operation_id,entity_type,entity_id,payload,received_at,result)` unique(actor_id,operation_id)
- `audit_logs(id,actor_id,workspace_id,action,resource_type,resource_id,metadata_redacted,created_at)`؛ append-only و retention policy.
- `ai_requests(id,user_id,provider,consent_version,input_hash,status,token_usage,cost,created_at,retention_until)`؛ متن prompt پیش‌فرض نگه‌داری نشود.
- `plans(id,code,limits_json,active)`, `subscriptions(id,workspace_id,plan_id,status,period_start,period_end,provider_ref)`
- `payments(id,subscription_id,provider,provider_ref,amount_minor,currency,status,idempotency_key,verified_at)` unique(provider,provider_ref); `invoices(id,payment_id,...)`, `support_tickets(id,user_id,status,subject,...)`.

### سیاست امنیت/نگهداری اولیه
متن وظیفه و پیوست فقط برای همان workspace و طبق ACL قابل دسترسی. مدیر عملیات پیش‌فرض payload خصوصی نمی‌بیند. audit log محتوای کاربر را ثبت نمی‌کند. حذف حساب: soft-delete محدود و سپس purge زمان‌بندی‌شده طبق سیاست حقوقی؛ backupها تا انقضای چرخه نگه می‌مانند و حذف فوری از backup تضمین نمی‌شود مگر معماری crypto-shredding. retention دقیق با مشاور حقوقی/امنیت نهایی شود. رمزنگاری در انتقال TLS و در ذخیره‌سازی دیسک/backup؛ برای داده حساس field-level encryption با KMS و کلید خارج کد.

## Offline-first و حل تعارض
- Room منبع حقیقت UI؛ ایجاد/ویرایش تراکنشی، یک رکورد outbox پایدار با UUID operation و base version.
- `POST /v1/sync/operations`، idempotency `(actor, operation_id)`؛ پاسخ canonical و mapping/temp id در صورت لزوم.
- sync incremental با cursor opaque؛ cursor فقط بعد از commit تغییرات دریافت‌شده ذخیره شود.
- update optimistic با `version`; تعارض: فیلدهای مستقل merge، متن/وضعیت/تاریخ تعارض حساس → نگه‌داشت هر دو نسخه و درخواست انتخاب کاربر. LWW کور برای حذف/زمان‌بندی ممنوع.
- حذف tombstone تا همهٔ کلاینت‌های فعال/دورهٔ retention؛ retry exponential backoff + jitter، سقف و نشانگر وضعیت؛ سرور push را خبر می‌دهد نه payload مرجع.
- فایل‌ها ابتدا صف محلی و پس از اتصال آپلود؛ quota و پاک‌سازی قابل تنظیم.

## تاریخ و recurrence
ذخیره‌سازی زمان لحظه‌ای UTC، رویداد تمام‌روزه به‌صورت local date interval و timezone. زمان برنامه‌ریزی منطقه‌ای به `ZoneId` و در سرور `timestamptz` با timezone نامی. تکرار ماهانهٔ روز ۳۱: policy صریح (آخرین روز ماه یا skip)؛ پیش‌فرض نمایشی: آخرین روز، قابل انتخاب. محاسبهٔ سال شمسی کبیسه و تبدیل فقط با کتابخانهٔ آزموده/نسخه‌بندی و آزمون مرجع؛ مناسبت قمری اعلامی، feed رسمی قابل نسخه‌بندی و override قابل ممیزی، نه پیش‌بینی قطعی. DST و تغییر timezone دستگاه سناریوی تست.

## API ابتدایی
در `services/api/openapi.yaml`: endpointهای health و افزودن/گرفتن وظیفهٔ local-sync pilot. هنوز auth و sync واقعی پیاده‌سازی نشده‌اند. همهٔ APIهای بعدی باید schema، pagination، error envelope، version و idempotency را مشخص کنند.
