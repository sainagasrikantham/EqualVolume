package com.csapps.equalvolume;

import android.content.Context;
import android.os.AsyncTask;

//The definition of our task class
public class ForeverTask extends AsyncTask<Void, Void, Void> {

   /* Constants */
   static final int FIVE_MINUTES_DELAY = 300000;
   
   /* System resources */
   private Context      mContext = null;

   public ForeverTask(Context context) {

      // Save the context
      mContext = context;
   }

   @Override
   protected void onPreExecute() {
      super.onPreExecute();
   }

   @Override
   protected Void doInBackground(Void...params) {

      while (true) {
         try {
            Thread.sleep(FIVE_MINUTES_DELAY);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }

         // Make provision for the callback to be called
         publishProgress();
      }
   }

   @Override
   protected void onProgressUpdate(Void...params) {
      super.onProgressUpdate(params);

      // Call the forever task callback
      ((MainActivity)mContext).foreverTaskCallback();
   }

   @Override
   protected void onPostExecute(Void params) {
      super.onPostExecute(params);
   }
}
