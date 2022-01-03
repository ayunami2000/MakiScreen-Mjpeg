/*
	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

	N.B.  the above text was copied from http://www.gnu.org/licenses/gpl.html
	unmodified. I have not attached a copy of the GNU license to the source...

    Copyright (C) 2011-2012 Timo Rantalainen
*/

/*
Written by Timo Rantalainen tjrantal@gmail.com 2010 (C++ version) - 2012 (Java version)
Based on Anssi Klapuri's (list of publications http://www.cs.tut.fi/~klap/iiro/ and http://www.elec.qmul.ac.uk/people/anssik/publications.htm) congress publication
Klapuri, A., " Multiple fundamental frequency estimation by summing harmonic amplitudes," 7th International Conference on Music Information Retrieval (ISMIR-06), Victoria, Canada, Oct. 2006.
http://www.cs.tut.fi/sgn/arg/klap/klap2006ismir.pdf
 and doctoral thesis:
Klapuri, A. " Signal processing methods for the automatic transcription of music," Ph.D. thesis, Tampere University of Technology, Finland, April 2004.
 http://www.cs.tut.fi/sgn/arg/klap/phd/klap_phd.pdf

Contributions from other people taken from the internet (in addition to Java-tutorials for GUI, sound capture etc.)
FFT-transform

Required class files (in addition to this one..).
ReadStratecFile.java		//Stratec pQCT file reader
DrawImage.java				//Visualize image in a panel
SelectROI.java
AnalyzeRoi.java			//Analysis calculations

JAVA compilation:
javac -cp '.:' ui/PolyphonicPitchDetection.java \
Capture/Capture.java \
DrawImage/DrawImage.java \
Analysis/Analysis.java \
Analysis/Complex.java \
Analysis/FFT.java \
Analysis/Functions.java \
Analysis/Klapuri.java
JAR building:
jar cfe PolyphonicPitchDetection.jar ui.PolyphonicPitchDetection ui DrawImage Analysis Capture

*/
package cat.maki.MakiScreen;
import cat.maki.MakiScreen.AudioParser.Analysis.Klapuri;
import cat.maki.MakiScreen.AudioParser.Capture;

import java.lang.Math;
import java.util.ArrayList;

public class PitchDetection {
    public int fftWindow = 4096;	/*FFT window width ~0.1 s -> Max ~600 bpm*/
    public float samplingRate = 44100;
    public static int harmonics = 20;
    public boolean continueCapturing;
    public double[] cb;			/*Klapuri whitening ranges*/
    public ArrayList<Double>[] Hb;	/*filter bank for whitening*/
    public ArrayList<Integer>[] hbIndices;	/*filter bank indices for whitening*/
    public double[] freq;		/*FFT fequency bins*/
    public double[] f0cands;	/*Klapuri F0 candidates*/
    public ArrayList<Integer>[] f0index;		/*Klapuri F0 candidate indices*/
    public ArrayList<Integer>[] f0indHarm;		/*Klapuri F0 candidate indices harmonics*/
    public PitchDetection(){ /*Constructor*/

    }

    public void beginPitchDetection(){
        /*Create constant arrays for Klapuri*/
        cb = new double[32];
        /*CB filterbank always the same values, could be included from somewhere...*/
        for (int b = 0;b<32;++b){
            cb[b] = 229.0*(Math.pow(10.0,(((double) (b+1.0))/21.4))-1.0); //frequency division
        }
			/*Frequencies, always the same after capture init...
			captured signal will be zero padded to twice its length, so valid fft bins are equal to original epoch length
			*/
        freq = new double[(int) Math.floor((double) fftWindow)];
        for (int b = 0;b<Math.floor((double) fftWindow);++b){
            freq[b] = (double) b*(double)(samplingRate/2.0)/(double) fftWindow;
        }

        /*Create filter bank*/
        Hb = new ArrayList[30];
        hbIndices = new ArrayList[30];
        for (int i = 1;i<31;++i){
            Hb[i-1] = new ArrayList<Double>();
            hbIndices[i-1] = new ArrayList<Integer>();
            int kk= Klapuri.ind(freq,cb[i-1]);
            while (freq[kk] <= cb[i+1]){
                hbIndices[i-1] .add(kk);
                if (freq[kk]<=cb[i]){
                    Hb[i-1].add(1-Math.abs(cb[i]-freq[kk])/(cb[i]-cb[i-1]));
                }else{
                    Hb[i-1].add(1-Math.abs(cb[i]-freq[kk])/(cb[i+1]-cb[i]));
                }
                ++kk;
            }
        }

        /*
         *Create candidate frequencies here (http://www.phy.mtu.edu/~suits/NoteFreqCalcs.html)
         *Five octaves of candidate notes. Use quarter a half-step to get out of tune freqs
         *Lowest freq (f0) = 55.0 Hz, A three octaves below A above the middle C
         */
        double f0Init = 55;	//Hz
        double a = Math.pow(2.0,(1.0/12.0));
        f0cands = new double[5*12*4];	//5 octaves, 12 half-steps per octave, quarter half-steps
        for (int kk = 0;kk<f0cands.length;++kk){
            f0cands[kk] = f0Init*Math.pow(a,((double)kk)/4.0);
        }

        /*
         *Pre-calculate frequency bins for  a given f0 candidate
         */
        f0index = new ArrayList[f0cands.length];
        f0indHarm = new ArrayList[f0cands.length];
        double halfBinWidth= ((double)samplingRate/(double) fftWindow)/2.0;
        for (int k =0;k<f0index.length;++k){
            f0index[k] = new ArrayList();
            f0indHarm[k] = new ArrayList();
            for (int h =0; h< harmonics;++h){
                ArrayList<Integer> tempInd =find(freq,f0cands[k]*((double)h+1.0)-halfBinWidth,f0cands[k]*((double)h+1.0)+halfBinWidth);
                f0index[k].addAll(tempInd);
                for (int t = 0;t<tempInd.size();++t){
                    f0indHarm[k] .add(h+1);
                }
            }
        }



        continueCapturing = true;
        /*Start capturing and analysis thread*/
        //Capture capture = new Capture(samplingRate,8,this);
        Capture capture = new Capture(16,this);
        Thread captureThread = new Thread(capture,"captureThread");
        captureThread.start();
    }

    public void endPitchDetection(){
        continueCapturing = false;
    }

    private ArrayList<Integer> find(double[] arr, double lower, double upper){
        ArrayList<Integer> b = new ArrayList<Integer>();
        for (int i = 0; i<arr.length;++i){
            if (arr[i]>=lower && arr[i] <=upper){
                b.add(i);
            }
        }
        return b;
    }
}


