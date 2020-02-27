package com.example.cinemastagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.cinemastagram.R
import com.example.cinemastagram.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null
    var auth : FirebaseAuth? = null    // 유저 정보 가지고 올 수 있도록 추가
    var firestore : FirebaseFirestore? = null    // 데이터 베이스 사용할 수 있도록 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // 초기화하기
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Activity화면이 열리자마자 오픈해 줄 수 있는 코드
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)

        // 버튼에 이벤트 넣어주기
        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }


    // 선택한 이미지 받는 부분
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){
                // 사진을 선택했을 때, 이미지의 경로가 이쪽으로 넘어옴
                photoUri = data?.data   // 경로 받아주기
                addphoto_image.setImageURI(photoUri)    //이미지 표시하기
            }else{
                // 취소버튼 눌렀을 때 작동하는 부분
                finish()
            }
        }
    }

    fun contentUpload() {
        // 파일업로드에는 2가지 방식 존재: 1. Callback / 2. Promise (구글에서 권장하는 방식)

        // 파일 이름 입력해 주는 코드
        // 이름이 중복되지 않도록 날짜값을 파일명으로 받는 코드로 만들어줌
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"

        // storage reference만들어서 이미지 업로드 하기
        var stroageRef = storage?.reference?.child("images")?.child(imageFileName)


        // ----------------------2. Promise 방식----------------------
        stroageRef?.putFile(photoUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask stroageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()

            // 이미지의 다운로드 경로 넣어주기
            contentDTO.imageUrl = uri.toString()

            // 유저의 uid 넣어주기
            contentDTO.uid = auth?.currentUser?.uid

            // uerId 넣어주기
            contentDTO.userId = auth?.currentUser?.email

            // content설명 넣어주기
            contentDTO.explain = addphoto_edit_explain.text.toString()

            // 시간 넣어주기
            contentDTO.timestamp = System.currentTimeMillis()

            // contentDTO를 images 컨텐트 안에다가 넣어주기
            firestore?.collection("images")?.document()?.set(contentDTO)

            setResult(Activity.RESULT_OK)       // 정상적으로 창이 닫혔다라는 플래그 값을 넘겨주기 위한 코드

            finish()        // 업로드 완료 후, finish를 이용하여 창 닫아주기

        }

//        // ----------------------1. Callback 방식----------------------
//        // 파일 업로드, 데이터베이스를 입력해 줄 코드
//        stroageRef?.putFile(photoUri!!)?.addOnSuccessListener {
//            // 이미지 업로드 완료 후, 이미지 주소 받아오는 코드
//            stroageRef.downloadUrl.addOnSuccessListener { uri ->
//                var contentDTO = ContentDTO()     // 이미지 주소 받아오자마자 데이터 모델 만들기
//
//                // 이미지의 다운로드 경로 넣어주기
//                contentDTO.imageUrl = uri.toString()
//
//                // 유저의 uid 넣어주기
//                contentDTO.uid = auth?.currentUser?.uid
//
//                // uerId 넣어주기
//                contentDTO.userId = auth?.currentUser?.email
//
//                // content설명 넣어주기
//                contentDTO.explain = addphoto_edit_explain.text.toString()
//
//                // 시간 넣어주기
//                contentDTO.timestamp = System.currentTimeMillis()
//
//                // contentDTO를 images 컨텐트 안에다가 넣어주기
//                firestore?.collection("images")?.document()?.set(contentDTO)
//
//                setResult(Activity.RESULT_OK)       // 정상적으로 창이 닫혔다라는 플래그 값을 넘겨주기 위한 코드
//
//                finish()        // 업로드 완료 후, finish를 이용하여 창 닫아주기
//            }
//        }


    }
}

