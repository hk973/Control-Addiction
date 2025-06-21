package com.genzopia.addiction.Launcher;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;


// Finally, modify ReviewActivity to not permanently mark reviews as shown
public class ReviewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use Google Play's Review API
        ReviewManager manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Launch the review flow
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(task2 -> finish()); // Close activity after review

                // REMOVED: We no longer set a permanent flag
                // Instead we're tracking the time in MainContainerActivity
            } else {
                finish(); // Close if there's an error
            }
        });
    }
}