package plugins.praveen.PSF;

import plugins.praveen.fft.AssignFunction2D;
import plugins.praveen.fft.AssignFunctions;
import plugins.praveen.fft.ComplexFunctions;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;

public class PSFCalculator {
	private int _w;
	private int _h;
	private int _z;
	private double _indexImmersion;
	private double _na;
	private int _lem;
	private double _indexSpRefr;
	private double _xySampling;
	private double _zSampling;
	private double _depth;
	
	public final static int DEFAULT_W = 128;
	public final static int DEFAULT_H = 128;
	public final static int DEFAULT_Z = 128;
	public final static double DEFAULT_INDEXIMMERSION = 1.515;
	public final static double DEFAULT_NA = 1.4;
	public final static int DEFAULT_LEM = 520;
	public final static double DEFAULT_INDEXSP = 1.33;
	public final static double DEFAULT_XYSAMPLING = 92.00;
	public final static double DEFAULT_ZSAMPLING = 277.00;
	public final static double DEFAULT_DEPTH = 0.0;
	PSFCalculator() {
		setW(DEFAULT_W);
		setH(DEFAULT_H);
		setZ(DEFAULT_Z);
		setXYSAMPLING(DEFAULT_XYSAMPLING);
		setZSAMPLING(DEFAULT_ZSAMPLING);
		setIndexImmersion(DEFAULT_INDEXIMMERSION);
		setNA(DEFAULT_NA);
		setLEM(DEFAULT_LEM);
		setIndexSp(DEFAULT_INDEXSP);
		
		setDEPTH(DEFAULT_DEPTH);		
	}
	public Sequence compute(){
		
		final DoubleFFT_2D fft = new DoubleFFT_2D(_w, _h);
		//IcyBufferedImage psf3d = new IcyBufferedImage(_w, _h, 1, DataType.DOUBLE);
		
		int hc = _h/2;
		int wc = _w/2;
		int zc = _z/2;
		double kSampling = (2*Math.PI)/(_h*_xySampling); //Fourier space sampling
		double lambdaObj = _lem/_indexImmersion;//Wavelength of light inside the medium
		double lambdaSp = _lem/_indexSpRefr;//Wavelength of light inside the medium
		double k0 = (2*Math.PI)/_lem;//Wave vector
		double kObj = (2*Math.PI)/lambdaObj;//Wave vector in the immersion medium
		double kSp = (2*Math.PI)/lambdaSp;//Wave vector in the medium
		double kMax = (2*Math.PI*_na)/(_lem*kSampling);//Maximum aperture radius
		//double saCoeff = _depth * (_indexSpRefr-_indexImmersion);
		
		// Define the zero defocus pupil function
		IcyBufferedImage pupil = new IcyBufferedImage(_w, _h, 2, DataType.FLOAT); // channel 1 is real and channel 2 is imaginary
		float[] pupilRealBuffer = pupil.getDataXYAsFloat(0);//Real
		float[] pupilImagBuffer = pupil.getDataXYAsFloat(1);//imaginary
		
		IcyBufferedImage dpupil = new IcyBufferedImage(_w, _h, 2, DataType.DOUBLE); // channel 1 is real and channel 2 is imaginary
		double[] dpupilRealBuffer = dpupil.getDataXYAsDouble(0); //Real
		double[] dpupilImagBuffer = dpupil.getDataXYAsDouble(1); //imaginary
		
		//Calculate the cosine and the sine components
		IcyBufferedImage ctheta = new IcyBufferedImage(_w, _h, 1, DataType.DOUBLE);
		double[] cthetaBuffer = ctheta.getDataXYAsDouble(0);
		IcyBufferedImage cthetaSp = new IcyBufferedImage(_w, _h, 1, DataType.DOUBLE);
		double[] cthetaSpBuffer = cthetaSp.getDataXYAsDouble(0);
		IcyBufferedImage stheta = new IcyBufferedImage(_w, _h, 1, DataType.DOUBLE);
		double[] sthetaBuffer = stheta.getDataXYAsDouble(0);
		IcyBufferedImage sthetaSp = new IcyBufferedImage(_w, _h, 1, DataType.DOUBLE);
		double[] sthetaSpBuffer = sthetaSp.getDataXYAsDouble(0);
		
		Sequence psf3d = new Sequence();
		psf3d.setName("WideField PSF");
        
		for (int k =  0 ; k < _z; k++)
    	{// Define the defocus pupils			
			
			double defocus = k-zc;
			defocus = defocus*_zSampling;	
			
			for(int x = 0; x < _w; x++)
			{
				for(int y = 0; y < _h; y++)
				{   
					double kxy = Math.sqrt( Math.pow(x-wc, 2) + Math.pow(y-hc, 2) );
        		
					pupilRealBuffer[pupil.getOffset(x, y)] = ((kxy < kMax) ? 1 : 0); //Pupil bandwidth constraints
					pupilImagBuffer[pupil.getOffset(x, y)] = 0; //Zero phase 
        		
					sthetaBuffer[x + y * _w] = Math.sin( kxy * kSampling / kObj );
					sthetaBuffer[x + y * _w] = (sthetaBuffer[x + y * _w]< 0) ? 0: sthetaBuffer[x + y * _w];
					sthetaSpBuffer[x + y * _w] = Math.sin( kxy * kSampling / kSp );
					sthetaSpBuffer[x + y * _w] = (sthetaSpBuffer[x + y * _w]< 0) ? 0: sthetaSpBuffer[x + y * _w];
					cthetaBuffer[x + y * _w] = Double.MIN_VALUE + Math.sqrt(1 - Math.pow(sthetaBuffer[x + y * _w], 2));
					cthetaSpBuffer[x + y * _w] = Double.MIN_VALUE + Math.sqrt(1 - Math.pow(sthetaSpBuffer[x + y * _w], 2));
					dpupilRealBuffer[x + y * _w] = pupilRealBuffer[pupil.getOffset(x, y)] * Math.cos((defocus * k0 * cthetaBuffer[x + y * _w]) + (k0 * _depth * (_indexSpRefr * cthetaSpBuffer[x + y * _w]-_indexImmersion * cthetaBuffer[x + y * _w])));
					dpupilImagBuffer[x + y * _w] = pupilRealBuffer[pupil.getOffset(x, y)] * Math.sin((defocus * k0 * cthetaBuffer[x + y * _w]) + (k0 * _depth * (_indexSpRefr * cthetaSpBuffer[x + y * _w]-_indexImmersion * cthetaBuffer[x + y * _w])));
        		}
			}
			double[] psf2d = dpupil.getDataCopyCXYAsDouble();
			fft.complexInverse(psf2d, false);
			
			IcyBufferedImage timg = new IcyBufferedImage(_w, _h, 1, DataType.DOUBLE);
			double[] timgData = timg.getDataXYAsDouble(0);
			
			AssignFunction2D assignFunction = new AssignFunctions.SwapAssign2D();
			assignFunction.assign(psf2d, timgData, _w, _h, new ComplexFunctions.Magnitude());
			
			timg.dataChanged();
			
            psf3d.addImage(timg);
            // TO DO: add normalization
    	}
		
	return psf3d;

	}
	public int getW() {
		return _w;
	}
	public int getH() {
		return _h;
	}
	public int getZ() {
		return _z;
	}
	public double getIndexRefr() {
		return _indexImmersion;
	}
	public double getNA() {
		return _na;
	}
	public int getLEM() {
		return _lem;
	}
	public double getSA() {
		return _indexSpRefr;
	}
	public double getXYSAMPLING() {
		return _xySampling;
	}
	public double getZSAMPLING() {
		return _zSampling;
	}
	public double getDEPTH() {
		return _depth;
	}
	
	public void setW(int src) {
		_w = src;
	}
	public void setH(int src) {
		_h = src;
	}
	public void setZ(int src) {
		_z = src;
	}
	public void setIndexImmersion(double src) {
		_indexImmersion = src;
	}
	public void setNA(double src) {
		_na = src;
	}
	public void setLEM(int src) {
		_lem = src;
	}
	public void setIndexSp(double src) {
		_indexSpRefr = src;
	}
	public void setXYSAMPLING(double src) {
		_xySampling = src;
	}
	public void setZSAMPLING(double src) {
		_zSampling = src;
	}
	public void setDEPTH(double src) {
		_depth = src;
	}
}
