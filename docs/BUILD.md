# GitHub Actions দিয়ে APK বিল্ড

## ১. ডিবাগ APK (সাইনিং ছাড়া, টেস্টের জন্য)
1. GitHub-এ নতুন রিপো বানান এবং এই প্রজেক্টের সব ফাইল (`.github` ফোল্ডারসহ) `main` ব্রাঞ্চে পুশ করুন।
2. রিপোর **Actions** ট্যাবে "Android Build" চলবে। শেষ হলে রানের পেজের নিচে **Artifacts → MuslimDaily-debug** ডাউনলোড করুন, ভেতরে APK আছে।
3. বিল্ড ফেইল করলে ব্যর্থ ধাপের লগের এরর লাইনগুলো কপি করে আমাকে পাঠান।

## ২. সাইন করা রিলিজ APK
কীস্টোর একবারই বানাবেন এবং নিরাপদ জায়গায় ব্যাকআপ রাখবেন (হারালে আপডেট দেওয়া যাবে না, আর রিপোতে কখনো কমিট করবেন না):

```
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias muslimdaily
base64 -w0 release.jks      # macOS: base64 -i release.jks
```

রিপো → **Settings → Secrets and variables → Actions → New repository secret** এ ৪টি সিক্রেট যোগ করুন:

| নাম | মান |
|---|---|
| `KEYSTORE_BASE64` | উপরের `base64` কমান্ডের আউটপুট |
| `KEYSTORE_PASSWORD` | কীস্টোরের পাসওয়ার্ড |
| `KEY_ALIAS` | `muslimdaily` (বা আপনার দেওয়া alias) |
| `KEY_PASSWORD` | কী-এর পাসওয়ার্ড |

রিলিজ বিল্ড চালানোর দুই উপায়:
- **Actions → Android Build → Run workflow**, তারপর "Also build the signed release APK" টিক দিন।
- অথবা ট্যাগ পুশ করুন: `git tag v1.0 && git push origin v1.0`। এতে APK রিপোর **Releases** পেজেও যোগ হয়ে যাবে।

নতুন ভার্সন দেওয়ার আগে `app/build.gradle.kts` এ `versionCode` বাড়াবেন (আর `versionName` বদলাবেন)।

## নোট
- প্রজেক্টে Gradle Wrapper নেই, তাই workflow সরাসরি Gradle 8.10.2 ইনস্টল করে। চাইলে Android Studio থেকে wrapper বানিয়ে কমিট করতে পারেন।
- রিলিজ বিল্ডে কোড সংকোচন (minify) এখন বন্ধ রাখা হয়েছে। ডিভাইসে পুরো অ্যাপ টেস্টের পর চালু করা যাবে।
- অ্যাপে এখনো নিজস্ব লঞ্চার আইকন নেই, ডিফল্ট Android আইকন দেখাবে।
