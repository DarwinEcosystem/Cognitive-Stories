/**
  * This sketch demonstrates how to implement an AudioListener and then add and remove it from one of Minim's 
  * classes that support AudioListeners: AudioPlayer, AudioSample, AudioOutput, and AudioInput. 
  *
  * An AudioListener is an interface that you can implement in your own classes, which allows you to receive
  * sample buffers from sound generating classes immediately after they are generated. This is particularly 
  * useful when you are doing audio analysis because it ensures that you will only see each buffer of audio
  * once. If instead you perform your analysis in the draw method of your sketch, you might get less than 
  * perfect results because they audio samples may change while you are observing them. This happens because 
  * the audio is generated in a different thread of execution than draw is called from.
  *  
  * You can add an instance of an AudioListener to a sound generating object by using the addListener 
  * method of the class. If you want to remove a listener that you previously added, you call the
  * removeListener method, passing the listener you want to remove.
  *
  * Although possible, it is not advised that you add the same listener to more than one sound generating object. 
  * Your listener will be called any time any of the objects you've added it to have new samples. 
  * This means that the stream of samples the listener sees will likely be interleaved buffers of samples from 
  * all of the objects it is listening to, which is probably not what you want.
  */

import ddf.minim.*;

Minim            minim;
AudioPlayer      groove;
WaveformRenderer waveform;
boolean          listening;

// You'll notice that the three methods of this class are synchronized. This is because the samples methods 
// will be called from a different thread than the one instances of this class will be created in. That thread 
// might try to send samples to an instance of this class while the instance is in the middle of drawing the 
// waveform, which would result in a waveform made up of samples from two different buffers. Synchronizing 
// all the methods means that while the main thread of execution is inside draw, the thread that calls 
// samples will block until draw is complete. Likewise, a call to draw will block if the sample thread is inside 
// one of the samples methods. Hope that's not too confusing!

class WaveformRenderer implements AudioListener
{
  private float[] left;
  private float[] right;
  
  WaveformRenderer()
  {
    left = null; 
    right = null;
  }
  
  public synchronized void samples(float[] samp)
  {
    left = samp;
  }
  
  public synchronized void samples(float[] sampL, float[] sampR)
  {
    left = sampL;
    right = sampR;
  }
  
  synchronized void draw()
  {
    // we've got a stereo signal if right or left are not null
    if ( left != null && right != null )
    {
      noFill();
      stroke(255);
      beginShape();
      for ( int i = 0; i < left.length; i++ )
      {
        vertex(i, height/4 + left[i]*50);
      }
      endShape();
      beginShape();
      for ( int i = 0; i < right.length; i++ )
      {
        vertex(i, 3*(height/4) + right[i]*50);
      }
      endShape();
    }
    else if ( left != null )
    {
      noFill();
      stroke(255);
      beginShape();
      for ( int i = 0; i < left.length; i++ )
      {
        vertex(i, height/2 + left[i]*50);
      }
      endShape();
    }
  }
}

void setup()
{
  size(512, 200, P3D);
  
  minim = new Minim(this);
  groove = minim.loadFile("groove.mp3", 512);
  groove.loop();
  waveform = new WaveformRenderer();
}

void draw()
{
  background(0);
  
  if ( listening )
  {  
    waveform.draw();
  }
  
  if ( listening ) 
  {
    text("Press space to remove the listener", 10, 20 );
  }
  else
  {
    text("Press space to add the listener", 10, 20 );
  }
}

void keyPressed()
{
  if ( key == ' ' )
  {
    if ( !listening )
    {
      groove.addListener( waveform );
      listening = true;
    }
    else 
    {
      groove.removeListener( waveform );
      listening = false;
    }
  }
}