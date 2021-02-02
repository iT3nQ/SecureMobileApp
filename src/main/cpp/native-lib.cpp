#include <jni.h>
#include <string>
extern "C" JNIEXPORT jstring JNICALL
Java_com_quentin_securebankaccount_MainActivity_API(
        JNIEnv* env,
jobject /* this */) {
std::string api = "https://60102f166c21e10017050128.mockapi.io/labbbank/config";
return env->NewStringUTF(api.c_str());
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_quentin_securebankaccount_MainActivity_API2(
        JNIEnv* env,
        jobject /* this */) {
    std::string api2 = "https://60102f166c21e10017050128.mockapi.io/labbbank/accounts";
    return env->NewStringUTF(api2.c_str());
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_quentin_securebankaccount_MainActivity_ID(
        JNIEnv* env,
        jobject /* this */) {
    std::string id = "1";
    return env->NewStringUTF(id.c_str());
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_quentin_securebankaccount_MainActivity_Key(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = "4ZVkF%ùKKb3m*nH/";
    return env->NewStringUTF(key.c_str());
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_quentin_securebankaccount_MainActivity_MasterKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = "/B?E(H+M";
    return env->NewStringUTF(key.c_str());
}
