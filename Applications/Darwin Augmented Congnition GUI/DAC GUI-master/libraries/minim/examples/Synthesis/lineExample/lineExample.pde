/* lineExample<br/>
   is an example of how to use the Line UGen inside an instrument.
   it is also a good example of using Midi2Hz and the music 
   sequencing methods of Minim.
   <p>
   For more information about Minim and additional features, 
   visit http://code.compartmental.net/minim/
   <p>
   author: Anderson Mills<br/>
   Anderson Mills's work was supported by numediart (www.numediart.org)
*/

// import everything necessary to make sound.
import ddf.minim.*;
import ddf.minim.ugens.*;

// create all of the variables that will need to be accessed in
// more than one methods (setup(), draw(), stop()).
Minim minim;
AudioOutput out;

/* MidiSlideInstrument
   uses an Oscil to make a square wave.  However, a Line is also
   used to slide the frequency of the Oscil from a starting frequency
   to an ending frequency.  Midi2Hz is used to make that slide musical.
*/  
   
// Every instrument must implement the Instrument interface so 
// playNote() can call the instrument's methods.
class MidiSlideInstrument implements Instrument
{
  // create all variables that must be used throughout the class
  Oscil tone;
  Line  freqControl;
  Midi2Hz midi2Hz;
  ADSR adsrGate;

  // constructor for this instrument  
  MidiSlideInstrument(float begNote, float endNote, float amp )
  { 
    // create new instances of any UGen objects as necessary
    tone = new Oscil( begNote, amp, Waves.SQUARE );
    adsrGate = new ADSR( 1.0, 0.001, 0.0, 1.0, 0.001 );
    freqControl = new Line( 1.0, begNote, endNote );
    midi2Hz = new Midi2Hz();
    
    // patch everything together up to the final output
    // Here, the line is patched through the midi2Hz UGen into the tone frequency
    freqControl.patch( midi2Hz ).patch( tone.frequency );
    // and the tone is patched into an ADSR
    tone.patch( adsrGate );
  }
  
  // every instrument must have a noteOn( float ) method
  void noteOn( float dur )
  {
    // when the note is turned on, the line has to be told how long it will last
    freqControl.setLineTime( dur );
    // and be told to start now
    freqControl.activate();
    // the ADSR has to be patched tho the output
    adsrGate.patch( out );
    // and be told to start also
    adsrGate.noteOn();
  }
 
  // every instrument must have a noteOff() method 
  void noteOff()
  {
    // when the note ends, the ADSR has to be told to start the release
    adsrGate.noteOff();
    // and then after the release, the output should be unpatched
    adsrGate.unpatchAfterRelease( out );
  }
}

// setup is run once at the beginning
void setup()
{ 
  // initialize the drawing window
  size( 512, 200, P2D );
  
  // initialize the minim and out objects
  minim = new Minim( this );
  out = minim.getLineOut( Minim.MONO, 2048 );
  
  // pause time when adding a bunch of notes at once
  // This guarantees accurate timing between all notes added at once.
  out.pauseNotes();

  // set the tempo for the piece
  out.setTempo( 120.0 );
  
  // I want a pause before the music starts`
  out.setNoteOffset( 1.0 );
  
  // play a note or several with the MidiSlideInstrument
  // which basically slides a square wave between two frequencies using
  // MIDI (and therefore a logarithmic slide).
 
  // short pop
  out.playNote( 0.0, 0.1, new MidiSlideInstrument( 48, 48, 0.2 ) );

  // long slide up of one note
  out.playNote( 1.0, 2.00, new MidiSlideInstrument( 48, 60, 0.2 ) );
  
  // a few blips and chirps
  out.playNote( 1.0, 0.1, new MidiSlideInstrument( 87, 99, 0.2 ) );
  out.playNote( 1.5, 0.05, new MidiSlideInstrument( 99, 99, 0.2 ) );
  out.playNote( 3.0, 0.1, new MidiSlideInstrument( 87, 99, 0.2 ) );  
  out.playNote( 4.0, 0.1, new MidiSlideInstrument( 48, 48, 0.2 ) );
  out.playNote( 4.25, 0.1, new MidiSlideInstrument( 48, 48, 0.2 ) );
  
  // slide of a chord up 
  out.playNote( 5.0, 2.00, new MidiSlideInstrument( 48, 60, 0.2 ) );
  out.playNote( 5.0, 2.00, new MidiSlideInstrument( 60, 72, 0.2 ) );
  out.playNote( 5.0, 2.00, new MidiSlideInstrument( 67, 79, 0.2 ) );
  
  // two more blips
  out.playNote( 8.0, 0.1, new MidiSlideInstrument( 84, 84, 0.2 ) );
  out.playNote( 8.25, 0.05, new MidiSlideInstrument( 96, 84, 0.2 ) );

  // a 1 beat chord slide down
  out.playNote( 9.0, 1.00, new MidiSlideInstrument( 60, 55, 0.2 ) );
  out.playNote( 9.0, 1.00, new MidiSlideInstrument( 72, 67, 0.2 ) );
  out.playNote( 9.0, 1.00, new MidiSlideInstrument( 79, 74, 0.2 ) );
  
  // after a quarter beat pause, continue that chord down
  out.playNote( 10.25, 0.75, new MidiSlideInstrument( 55, 48, 0.2 ) );
  out.playNote( 10.25, 0.75, new MidiSlideInstrument( 67, 60, 0.2 ) );
  out.playNote( 10.25, 0.75, new MidiSlideInstrument( 74, 67, 0.2 ) );
  
  // more chirps
  out.playNote( 11.5, 0.05, new MidiSlideInstrument( 84, 96, 0.2 ) );
  out.playNote( 11.125, 0.05, new MidiSlideInstrument( 60, 48, 0.2 ) );
  out.playNote( 11.25, 0.05, new MidiSlideInstrument( 96, 48, 0.2 ) );

  // one long slow note down
  out.playNote( 12.00, 2.5, new MidiSlideInstrument( 60, 48, 0.2 ) );
  
  // joined by two others (this does not make a harmonic chord,
  // because the notes are all sliding to one common goal)
  out.playNote( 13.00, 1.5, new MidiSlideInstrument( 55, 48, 0.2 ) );
  out.playNote( 13.00, 1.5, new MidiSlideInstrument( 67, 48, 0.2 ) );
  
  // two constant notes with a sliding note
  out.playNote( 15.00, 2.0, new MidiSlideInstrument( 48, 48, 0.2 ) );  
  out.playNote( 15.00, 1.0, new MidiSlideInstrument( 36, 48, 0.2 ) );
  out.playNote( 15.00, 1.01, new MidiSlideInstrument( 36, 36, 0.1 ) );
  
  // more chirps
  out.playNote( 16.5, 0.05, new MidiSlideInstrument( 48, 36, 0.2 ) );
  out.playNote( 16.75, 0.05, new MidiSlideInstrument( 70, 48, 0.2 ) );
  
  // and final blips and chirps
  out.playNote( 17.50, 0.1, new MidiSlideInstrument( 96, 96, 0.2 ) );
  out.playNote( 18.00, 0.1, new MidiSlideInstrument( 72, 84, 0.1 ) );
  out.playNote( 18.00, 0.2, new MidiSlideInstrument( 84, 84, 0.2 ) );
  
  // resume time after adding all of these notes at once.
  out.resumeNotes();
}

// draw is run many times
void draw()
{
  // erase the window to black
  background( 0 );
  // draw using a white stroke
  stroke( 255 );
  // draw the waveforms
  for( int i = 0; i < out.bufferSize() - 1; i++ )
  {
    // find the x position of each buffer value
    float x1  =  map( i, 0, out.bufferSize(), 0, width );
    float x2  =  map( i+1, 0, out.bufferSize(), 0, width );
    // draw a line from one buffer position to the next for both channels
    line( x1, 50 + out.left.get(i)*50, x2, 50 + out.left.get(i+1)*50);
    line( x1, 150 + out.right.get(i)*50, x2, 150 + out.right.get(i+1)*50);
  }  
}
