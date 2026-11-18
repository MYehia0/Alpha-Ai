# SignInReason is a type-safe navigation argument (SignInRoute.reason). It lives in the JVM
# :core:domain module, which cannot use @Keep, so keep it here for minified builds.
-keep enum com.example.alpha_ai.core.domain.auth.SignInReason { *; }
