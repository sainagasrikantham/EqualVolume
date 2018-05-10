package com.csapps.equalvolume;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

public class VolumeObserver extends ContentObserver {

   /* Volume Stream Constants */
   static final int INVALID_STREAM              = -1;
   static final int VOLUME_STREAM_RING_TONE     =  0;
   static final int VOLUME_STREAM_MUSIC         =  1;
   static final int VOLUME_STREAM_NOTIFICATIONS =  2;
   static final int VOLUME_STREAM_SYSTEM_SOUNDS =  3;
   static final int VOLUME_STREAM_VOICE_CALLS   =  4;
   static final int MAX_VOLUME_STREAMS          =  5;

   /* Constants */
   static final private int ONE_SECOND          = 1000;

   /* Variables */
   private int []       mPreviousStreamVolume;

   /* System resources */
   private Context      mContext              = null;
   private AudioManager mAudioManager         = null;
   private Handler      mHandler              = null;
   private Runnable     mVolumeObserverThread = null;

   /* Logging Variables */
   private final boolean PRINT_LOG_STMTS = false;

   public VolumeObserver(Context context, Handler handler) {
      super(handler);

      // Save the context
      mContext = context;

      // Get the system Audio Manager
      mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

      // Create the "previous volume" array
      mPreviousStreamVolume = new int[MAX_VOLUME_STREAMS];

      // Fetch stream volumes
      for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {
         int osStreamID = (int)((MainActivity)mContext).getOSStreamID(streamID);
         mPreviousStreamVolume[streamID] = mAudioManager.getStreamVolume(osStreamID);
      }

      // Create the handler object
      mHandler = new Handler();

      // Volume Observer thread
      mVolumeObserverThread = new Runnable() {

         @Override
         public void run() {

            // Get the operating system stream ID of the changed stream (if any)
            int changedOSStreamID = getChangedOSStreamID();

            if ( changedOSStreamID != INVALID_STREAM ) {

               // Handle volume change
               handleVolumeChange(changedOSStreamID);

            } else {

               // Register for the next callback
               registerVolumeObserverHandler();
            }
         }
      };

      // Register the volume observer handler callback for the first time
      registerVolumeObserverHandler();

   } // End of constructor VolumeObserver

   // Creating the volume observer callback
   public void registerVolumeObserverHandler() {

      // Register the UI refresh thread with the handler
      mHandler.postDelayed ( mVolumeObserverThread, ONE_SECOND );

   } // End of function registerUIRefreshHandler()

   @Override
   public boolean deliverSelfNotifications() {
      return super.deliverSelfNotifications();
   }

   @Override
   public void onChange(boolean selfChange) {
      super.onChange(selfChange);
   } // End of onChange

   private void handleVolumeChange(int changedOSStreamID) {

      // Get the "delta" of the changed volume stream
      int delta = getChangedStreamDelta(changedOSStreamID);

      if ( delta > 0 ) {

         // Volume Up
         ((MainActivity)mContext).handleVolumeChange(true, changedOSStreamID);

      } else if ( delta <= 0 ) {

         // Volume Down
         ((MainActivity)mContext).handleVolumeChange(false, changedOSStreamID);
      }
   } // End of handleVolumeChange

   /***
    * 
    * @return the OS stream ID whose volume has changed
    */
   private int getChangedOSStreamID() {

      int changedOSStreamID = INVALID_STREAM;

      // Fetch stream volume and check against previous
      for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {
         int osStreamID          = (int)((MainActivity)mContext).getOSStreamID(streamID);
         int currentStreamVolume = mAudioManager.getStreamVolume(osStreamID);

         // The current and the previous do not match
         if (currentStreamVolume != mPreviousStreamVolume[streamID]) {

            // Changed OS stream ID
            changedOSStreamID = osStreamID;
            break;
         }
      }

      return changedOSStreamID;
   } // End of getChangedOSStreamID

   /***
    * The function returns the delta of the volume change of the passed OS stream ID
    * @param changedOSStreamID The id of the changed OS stream
    * @return the delta of the changed stream volume
    */
   private int getChangedStreamDelta(int changedOSStreamID) {

      int delta = 0;

      int streamID 			  = (int)((MainActivity)mContext).getEqualVolumeStreamID(changedOSStreamID);
      int currentStreamVolume = mAudioManager.getStreamVolume(changedOSStreamID);

      // Delta is "current volume" minus "previous volume"
      delta = currentStreamVolume - mPreviousStreamVolume[streamID];

      // Save the current value for "future" usage
      mPreviousStreamVolume[streamID] = currentStreamVolume;

      if ( PRINT_LOG_STMTS ) {
         Log.v("Equal Volume", "currentStreamVolume = "+currentStreamVolume);
         Log.v("Equal Volume", "delta               = "+delta);
      }

      return delta;
   } // End of getChangedStreamDelta

   // Updates the previous stream volume array with the current stream volumes
   public void updatePreviousVolumes() {

      // Fetch stream volume and check against previous
      for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {

         int osStreamID          = (int)((MainActivity)mContext).getOSStreamID(streamID);
         int currentStreamVolume = mAudioManager.getStreamVolume(osStreamID);

         // Save the current value for "future" usage
         mPreviousStreamVolume[streamID] = currentStreamVolume;
      }

   } // End of updatePreviousVolumes

} // End of public class VolumeObserver
