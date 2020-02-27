package com.example.cinemastagram.navigation.model

data class ContentDTO(var explain : String? = null,         // 컨텐츠의 데이터를 관리해주는 explain 만들기
                      var imageUrl : String? = null,        // 이미지 주소 관리하는 imageUrl 변수 만들기
                      var uid : String? = null,             // 어느 유저가 올렸는지 uid 만들기
                      var userId : String? = null,          //올린 유저의 이미지를 관리해주는 userId 변수 만들기
                      var timestamp : Long? = null,         //몇시 몇분에 컨텐츠 올렸는지 관리
                      var favoriteCount : Int = 0,          // 좋아요 몇개 눌렀는지 관리
                      var favorites : Map<String, Boolean> = HashMap()){        // 중복 좋아요 방지 <-- 좋아요 누른 유저 관리

    // 댓글 관리해주는 Comment 데이터 클래스
    data class Comment(var uid : String? = null,
                       var userId : String? = null,         // 이메일 관리
                       var comment : String? = null,        // comment 관리
                       var timestamp: Long? = null)         // 몇시 몇분에 comment 달았는지 관리





}




