package com.csapps.equalvolume;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends Activity {

   /* Volume Stream Constants */
   static final int INVALID_STREAM              = -1;
   static final int VOLUME_STREAM_RING_TONE     =  0;
   static final int VOLUME_STREAM_MUSIC         =  1;
   static final int VOLUME_STREAM_NOTIFICATIONS =  2;
   static final int VOLUME_STREAM_SYSTEM_SOUNDS =  3;
   static final int VOLUME_STREAM_VOICE_CALLS   =  4;
   static final int MAX_VOLUME_STREAMS          =  5;

   /* UI resources */
   private SeekBar [] 	 mStreamSeekBars;
   private CheckBox[]    mCheckBoxes;

   /* System resources */
   private AudioManager  mAudioManager = null;

   /* Variables */
   private int []        mStreamMaxVolumes;
   private int []        mAdjustSteps;
   private boolean       mCreationComplete = false;

   /* Volume Observer Resources */
   private VolumeObserver  mVolumeObserver  = null;
   private ForeverTask     mForeverTask     = null;
   private int             mCallCount       = 0;

   /* Logging Variables */
   private final boolean PRINT_LOG_STMTS   = true;
   
   /* Notification Resources */
   private Notification.Builder mEqualVolumeNB       = null;
   private NotificationManager  mNotificationManager = null;
   private int                  mNotificationID      = 0;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.activity_main);

      if ( !mCreationComplete ) {

         // Create stuff
         createEverything();
         
         // Start a forever running task
         startForeverTask();
         
         mCreationComplete = true;
      }
   }
   
   /* Starts the forever task */
   public void startForeverTask() {

      // Start the task
      mForeverTask.execute();
   }
   
   // The call back that is used to keep the app in memory and running
   public void foreverTaskCallback() {

      // Increment a place holder, useless, variable
      mCallCount++;
   }
   
   /** 
    * Returns the "EqualVolume" version of the 'stream ID'
    * @param osStreamID The value of the OS' version of the 'stream ID'
    * */
   public int getEqualVolumeStreamID(int osStreamID) {

      int equalVolStreamID = VOLUME_STREAM_RING_TONE;

      switch ( osStreamID ) {
         default:
         case AudioManager.STREAM_MUSIC:
            break;
         case AudioManager.STREAM_NOTIFICATION:
            equalVolStreamID = VOLUME_STREAM_NOTIFICATIONS;
            break;
         case AudioManager.STREAM_SYSTEM:
            equalVolStreamID = VOLUME_STREAM_SYSTEM_SOUNDS;
            break;
         case AudioManager.STREAM_VOICE_CALL:
            equalVolStreamID = VOLUME_STREAM_VOICE_CALLS;
            break;
         case AudioManager.STREAM_RING:
            osStreamID = VOLUME_STREAM_RING_TONE;
            break;
      }

      return equalVolStreamID;
   }

   /** 
    * Returns the OS' version of the 'stream ID'
    * @param equalVolStreamID The value of "EqualVolume" version of the 'stream ID'
    * */
   public int getOSStreamID(int equalVolStreamID) {

      int osStreamID = AudioManager.STREAM_MUSIC;

      switch ( equalVolStreamID ) {
         default:
         case VOLUME_STREAM_MUSIC:
            break;
         case VOLUME_STREAM_NOTIFICATIONS:
            osStreamID = AudioManager.STREAM_NOTIFICATION;
            break;
         case VOLUME_STREAM_SYSTEM_SOUNDS:
            osStreamID = AudioManager.STREAM_SYSTEM;
            break;
         case VOLUME_STREAM_VOICE_CALLS:
            osStreamID = AudioManager.STREAM_VOICE_CALL;
            break;
         case VOLUME_STREAM_RING_TONE:
            osStreamID = AudioManager.STREAM_RING;
            break;
      }

      return osStreamID;
   }

   /** 
    * Creates/gathers all the resources necessary for the app
    * @param none
    * */
   private void createEverything() {
      // Get the AudioManager
      mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
      
      // Get the NotificationManager
      mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      
      mEqualVolumeNB = new Notification.Builder(getApplicationContext())
      .setSmallIcon(R.drawable.ic_launcher)
      .setContentTitle("Equal Volume")
      .setContentText("Restart Application")
      .setTicker("App kicked out of memory by Android. Please restart.");

      // Create the seek bar array
      mStreamSeekBars = new SeekBar[MAX_VOLUME_STREAMS];
      mCheckBoxes     = new CheckBox[MAX_VOLUME_STREAMS];

      mStreamSeekBars[VOLUME_STREAM_RING_TONE]     = (SeekBar)findViewById(R.id.seekBar_ring_tone);
      mStreamSeekBars[VOLUME_STREAM_MUSIC]         = (SeekBar)findViewById(R.id.seekBar_music);
      mStreamSeekBars[VOLUME_STREAM_NOTIFICATIONS] = (SeekBar)findViewById(R.id.seekBar_notifications);
      mStreamSeekBars[VOLUME_STREAM_SYSTEM_SOUNDS] = (SeekBar)findViewById(R.id.seekBar_system_sounds);
      mStreamSeekBars[VOLUME_STREAM_VOICE_CALLS]   = (SeekBar)findViewById(R.id.seekBar_voice_calls);

      mCheckBoxes[VOLUME_STREAM_RING_TONE]         = (CheckBox)findViewById(R.id.checkBox_ring_tone);
      mCheckBoxes[VOLUME_STREAM_MUSIC]             = (CheckBox)findViewById(R.id.checkBox_music);
      mCheckBoxes[VOLUME_STREAM_NOTIFICATIONS]     = (CheckBox)findViewById(R.id.checkBox_notifications);
      mCheckBoxes[VOLUME_STREAM_SYSTEM_SOUNDS]     = (CheckBox)findViewById(R.id.checkBox_system_sounds);
      mCheckBoxes[VOLUME_STREAM_VOICE_CALLS]       = (CheckBox)findViewById(R.id.checkBox_voice_calls);

      // Create the max volume array
      mStreamMaxVolumes = new int[MAX_VOLUME_STREAMS];

      // Create the volume adjust steps array
      mAdjustSteps = new int[MAX_VOLUME_STREAMS];

      for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {

         // Fetch stream maximums
         mStreamMaxVolumes[streamID] = mAudioManager.getStreamMaxVolume(getOSStreamID(streamID));
         mAdjustSteps     [streamID] = 1;
         
         if ( streamID == VOLUME_STREAM_MUSIC ) {
            mAdjustSteps [streamID] = 2;
         }

         // Make the progress bars "read-only"
         mStreamSeekBars[streamID].setEnabled(false);

         // Check all the boxes
         mCheckBoxes[streamID].setChecked(true);

         if ( PRINT_LOG_STMTS ) Log.v("Equal Volume", "mStreamMaxVolumes["+streamID+"] = "+mStreamMaxVolumes[streamID]);
      }

      // Figure out the minimum volume 
      int minVol = mStreamMaxVolumes[VOLUME_STREAM_RING_TONE];
      for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {
         if ( mStreamMaxVolumes[streamID] < minVol ) {
            minVol = mStreamMaxVolumes[streamID];
         }
      }

      // Figure out adjust steps for each stream
      /*for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {
         mAdjustSteps[streamID] = (mStreamMaxVolumes[streamID] / minVol);

         if ( PRINT_LOG_STMTS ) Log.v("Equal Volume", "mAdjustSteps[= "+streamID+"] = "+mAdjustSteps[streamID]);
      }*/

      // Fetch stream maximums and set the seek bar maximums
      for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {
         mStreamMaxVolumes[streamID] = mAudioManager.getStreamMaxVolume(getOSStreamID(streamID));
         mStreamSeekBars[streamID].setMax(mStreamMaxVolumes[streamID]);

         if ( PRINT_LOG_STMTS ) Log.v("Equal Volume", "mStreamMaxVolumes["+streamID+"] = "+mStreamMaxVolumes[streamID]);
      }

      // Create the volume observer
      mVolumeObserver = new VolumeObserver(this, new Handler());

      // Update current volume levels UI
      updateCurrentVolumeLevels();
      
      // Create the forever task object
      mForeverTask = new ForeverTask(this);
   }

   /** 
    * Updates the volume levels of all the streams
    * @param none
    * */
   private void updateCurrentVolumeLevels() {
      for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {
         int currentVolume = mAudioManager.getStreamVolume(getOSStreamID(streamID));
         mStreamSeekBars[streamID].setProgress(currentVolume);

         if ( PRINT_LOG_STMTS ) Log.v("Equal Volume", "currentVolume of streamID ["+streamID+"] = "+currentVolume);
         if ( PRINT_LOG_STMTS ) Log.v("Equal Volume", "mStreamSeekBars["+streamID+"] progress = "+mStreamSeekBars[streamID].getProgress());
      }
   }

   @Override
   protected void onPostCreate(Bundle savedInstanceState) {
      super.onPostCreate(savedInstanceState);
   }

   @Override
   protected void onPause() {
      super.onPause();
      
   }
   
   @Override
   protected void onResume() {
      super.onResume();

      // Update current volume levels UI
      updateCurrentVolumeLevels();
   }

   @Override
   protected void onDestroy() {

      // Throw the notification
      mNotificationManager.notify(mNotificationID++, mEqualVolumeNB.build());
   }

   private void checkForSentinelLevels() {

      boolean anyStreamAtMax = false;
      boolean anyStreamAtMin = false;

      for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {

         int streamVolume = mAudioManager.getStreamVolume(getOSStreamID(streamID));

         if ( mCheckBoxes[streamID].isChecked() ) {
            if ( streamVolume == mStreamMaxVolumes[streamID]) {
               anyStreamAtMax = true;
            } else if (streamVolume == 0) {
               anyStreamAtMin = true;
            }
         }
      }

      if ( anyStreamAtMin || anyStreamAtMax ) {

         if ( anyStreamAtMax ) {

            for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {

               if ( mCheckBoxes[streamID].isChecked() ) {
                  mAudioManager.setStreamVolume(getOSStreamID(streamID), mStreamMaxVolumes[streamID], AudioManager.FLAG_VIBRATE);	
               }
            } // End of for

            Toast.makeText(this, "Volume Maximum!", Toast.LENGTH_SHORT).show();
         } else if ( anyStreamAtMin ) {

            for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {

               if ( mCheckBoxes[streamID].isChecked() ) {
                  mAudioManager.setStreamVolume(getOSStreamID(streamID), 0, AudioManager.FLAG_VIBRATE);
               }
            } // End of for

            Toast.makeText(this, "Volume Minimum!", Toast.LENGTH_SHORT).show();
         }

         // Update current volume levels UI
         updateCurrentVolumeLevels();
      }
   }

   /**
    * Handles the "volume" button key-presses
    * @param volumeUp Boolean variable which is 'true' if the volume has increased from before, 'false' otherwise
    */
   private void handleKeyPress(boolean volumeUp, int changedOSStreamID) {

      int changedEqualVolumeStreamID = getEqualVolumeStreamID(changedOSStreamID);
      
      for ( int streamID = VOLUME_STREAM_RING_TONE; streamID < MAX_VOLUME_STREAMS; ++streamID ) {

         if ( mCheckBoxes[streamID].isChecked() ) {

            // Skip the stream that caused the event
            if ( streamID != changedEqualVolumeStreamID ) {
               for ( int adjustSteps = 1; adjustSteps <= mAdjustSteps[streamID]; ++adjustSteps ) {
                  mAudioManager.adjustStreamVolume(getOSStreamID(streamID), ( volumeUp ) ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_VIBRATE);
               }
            }

            mStreamSeekBars[streamID].setProgress(mAudioManager.getStreamVolume(getOSStreamID(streamID)));
         } // End of if

      } // End of for
   }

   /**
    * Handles the volume change performed either within or outside the app
    * @param volumeUp Boolean variable which is 'true' if the volume has increased from before, 'false' otherwise
    */
   public void handleVolumeChange(boolean volumeUp, int changedOSStreamID) {

      if ( PRINT_LOG_STMTS ) {
         Log.v("Equal Volume", "*** handleVolumeChange() Called");
      }

      // Handle the key-press
      handleKeyPress(volumeUp, changedOSStreamID);

      // Check if any stream has reached "max" or "min"
      checkForSentinelLevels();

      // Update the previous volume array
      mVolumeObserver.updatePreviousVolumes();

      // Register for the next volume observer callback
      mVolumeObserver.registerVolumeObserverHandler();
   }

} // End of public class MainActivity
