package com.example.cinemastagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cinemastagram.R
import com.example.cinemastagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment(){

    //DB에 바로 접근 할 수 있도록 변수 생성
    var firestore : FirebaseFirestore? = null

    // uid받아오는 부분 글로벌로
    var uid : String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.detailviewitemfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewitemfragment_recyclerview.layoutManager = LinearLayoutManager(activity) // 화면을 세로로 배치하기 위해
        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()

        // 생성자
        init {
            // DB에 접근하여 데이터를 받아올 수 있는 쿼리 생성
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                if(querySnapshot == null)
                    return@addSnapshotListener

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()  // 값이 새로고침되도록 만들기
            }
        }
        override fun onCreateViewHolder(p0 : ViewGroup, p1 : Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_detail, p0, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            // 서버에서 넘어오는 데이터들을 매핑 시켜주는 부분
            var viewholder = (p0 as CustomViewHolder).itemView

            // UserId
            viewholder.detailviewitem_profile_textview.text = contentDTOs!![p1].userId

            //Image
            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(viewholder.detailviewitem_imageview_content)

            // Expalin of content
            viewholder.detailviewitem_explain_textview.text = contentDTOs!![p1].explain

            // likes
            viewholder.detailviewitem_favoritecounter_textview.text = "Likes " + contentDTOs!![p1].favoriteCount

            // ProfileImate
            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(viewholder.detailviewitem_profile_image)

            // 버튼 클릭했을 때
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(p1)
            }

            // 좋아요 카운트, 하트 색칠 or 비어있는 이벤트 발생시키는 코드
            if(contentDTOs!![p1].favorites.containsKey(uid)){
                // 좋아요 키 눌렸을 때
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{
                // 좋아요 키 눌리지 않았을 때
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }
            // profile이미지 클릭하게 되면 상대방 유저정보로 이동하는 코드
            viewholder.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[p1].uid)
                bundle.putString("userId", contentDTOs[p1].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.commit()
            }
        }

        fun favoriteEvent(postion : Int){
            // 선택한 컨텐츠의 UID를 받아와서 좋아요 해주는 이벤트
            var tsDoc = firestore?.collection("images")?.document(contentUidList[postion])
            // 데이터를 입력해줄 수 있는 transaction 만들우 주기
            firestore?.runTransaction { transaction ->
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)){
                    // 좋아요 버튼이 눌려있는 상태
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount - 1
                    contentDTO?.favorites.remove(uid)
                }else{
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount + 1
                    contentDTO?.favorites[uid!!] = true
                }
                // transaction 서버로 돌리기
                transaction.set(tsDoc, contentDTO)
            }

        }

    }
}