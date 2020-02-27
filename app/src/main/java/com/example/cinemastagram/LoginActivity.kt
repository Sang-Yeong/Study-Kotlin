package com.example.cinemastagram

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import com.google.android.gms.common.util.IOUtils.toByteArray
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Base64
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {

    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null     // 구글 로그인 클래스 만들기
    var GOOGLE_LOGIN_CODE = 9001    // 구글 로그인 할때 사용할 reqeust코드를 만들어 줌
    var callbackManager : CallbackManager? = null// facebook로그인 결과값 가져오는 콜백 매니저 만들기
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        //로그인 버튼에 대한 설정
        email_login_button.setOnClickListener {
            signinAndSignup()
        }

        // 구글 로그인 버튼에 대한 설정- step1(google)
        google_sign_in_button.setOnClickListener {
            googleLogin()
        }

        // 페이스북 로그인 버튼에 대한 설정- step1(facebook)
        facebook_login_button.setOnClickListener {
            facebookLogin()
        }



        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()//구글 로그인 옵션 만들기
        googleSignInClient = GoogleSignIn.getClient(this,gso)   //googleSignInCilent에 세팅하기

        // printHashKey() Oncreate문에 넣어주기
        // printHashKey()

        // 페이스북 콜백 매니저 --> 이 결과값은 onActivityResult에 넘어감
        callbackManager = CallbackManager.Factory.create()
    }

    // key hash facebook android: HC0HQet27GqqLDb2Hjs++7VZCjQ=
    fun printHashKey() {
        try {
            val info = packageManager.getPackageInfo(packageName,PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(), 0))
                Log.i("TAG", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("TAG", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("TAG", "printHashKey()", e)
        }

    }

    // 구글 로그인 코드
    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent,GOOGLE_LOGIN_CODE)
    }

    // 페이스북 로그인
    fun facebookLogin(){
        //페이스북에서 받을 권한 요청
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        // callbackManager 등록
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult?) {
                    // 로그인 성공 시- step2(facebook)
                    handleFacebookAccessToken(result?.accessToken)
                }

                override fun onCancel() {

                }

                override fun onError(error: FacebookException?) {

                }

            })
    }

    fun handleFacebookAccessToken(token : AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        // 응답 값을 받아 파이어베이스로 넘겨주는 부분
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){
                    // 아이디와 패스워드가 맞아 로그인에 성공한 경우- step3(facebook)
                    moveMainPage(task.result?.user)
                }
                else{
                    // 로그인이 실패한 경우
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    // 구글 로그인 후, 값을 파이어베이스로 넘기는 과정
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 콜백 매니저 넘겨주는 코드(페이스북)
        callbackManager?.onActivityResult(requestCode, resultCode,data)
        if(requestCode == GOOGLE_LOGIN_CODE){
            // 구글에서 넘겨주는 결과값 받아오기
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess){
                var account = result.signInAccount
                // 구글 로그인 step2
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            }
        }
    }
    // firebaseAuthWithGoogle 함수: 타입이 GoogleSignInAccount인 account 파라미터
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount){
        var credential = GoogleAuthProvider.getCredential(account?.idToken,null)    // account안에 있는 토큰 아이디를 넘겨준다.
        auth?.signInWithCredential(credential)

            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){
                    // 아이디와 패스워드가 맞아 로그인에 성공한 경우
                    moveMainPage(task.result?.user)
                }
                else{
                    // 로그인이 실패한 경우
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    // 회원가입 코드
    fun signinAndSignup(){
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())?.addOnCompleteListener {
                task ->
            if(task.isSuccessful){
                // 아이디가 생성되었을 때 작동하는 부분, 회원가입
                moveMainPage(task.result?.user)
            }
            else if(task.exception?.message.isNullOrEmpty()){
                // 로그인이 정상적으로 작동하지 않을 때
                // Toast.makeText(this, "정상적인 작동 X",Toast.LENGTH_LONG).show()
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
            else{
                // 로그인 하는 부분
                signinEmail()
            }
        }
        // 로그인에서 첫번째 파라미터 == 이메일 입력, 두번째 파라미터 == 비밀번호 입력
        // 회원가입 한 결과값을 받아오기 위해 addOnCompleteListener{}
    }

    // 로그인 하는 코드
    fun signinEmail() {
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())
            ?.addOnCompleteListener {
                task ->
            if(task.isSuccessful){
                // 아이디와 패스워드가 맞아 로그인에 성공한 경우
                moveMainPage(task.result?.user)
            }
            else{
                // 로그인이 실패한 경우
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }

    }

    // 로그인 성공시, 다음 페이지로 넘어가는 함수
    fun moveMainPage(user: FirebaseUser?){

        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
        }

    }
}

