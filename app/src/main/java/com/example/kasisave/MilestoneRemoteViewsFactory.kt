package com.example.kasisave

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MilestoneRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var milestones: List<Milestone> = listOf()

    override fun onCreate() {}

    override fun onDataSetChanged() {

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val snapshot = Tasks.await(
            db.collection("milestones")
                .whereEqualTo("userId", userId)
                .get()
        )

        milestones = snapshot.documents.mapNotNull { it.toObject(Milestone::class.java) }

    }

    override fun onDestroy() {
        milestones = emptyList()
    }

    override fun getCount(): Int = milestones.size

    override fun getViewAt(position: Int): RemoteViews {
        val milestone = milestones[position]
        val views = RemoteViews(context.packageName, R.layout.milestone_widget_item)
        views.setTextViewText(R.id.textMilestoneName, milestone.name)
        views.setTextViewText(R.id.textMilestoneAmount, "R ${milestone.targetAmount}")
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
