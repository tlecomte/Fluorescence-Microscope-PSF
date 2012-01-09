package plugins.praveen.PSF;

import icy.gui.dialog.MessageDialog;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarText;

public class NyquistSampling extends EzPlug {
	EzVarText _mname = new EzVarText("Choose your microscope", new String[] {  "Wide-Field", "Confocal" }, 1, false);
	EzVarInteger _nPhotons = new EzVarInteger("Number of photons used to excite the fluorophore", 1, 2, 1);
	EzVarInteger _lex = new EzVarInteger("Excitation peak wavelength, in nm", 400, 750, 1);	
	EzVarInteger _lem = new EzVarInteger("Emission peak wavelength, in nm", 405, 750, 1);		
	EzVarDouble _indexImmersion = new EzVarDouble("Refractive index of the medium between lens and cover slip", 1.00, 4.00, 0.01);
	EzVarDouble _objNA = new EzVarDouble("Effective numerical aperture of the objective lens", 0.1, 4.00, 0.01);
	EzVarDouble _xySampling = new EzVarDouble("Radial Nyquist Sampling is (in nm)");
	EzVarDouble _zSampling = new EzVarDouble("Axial Nyquist Sampling is (in nm)");



	@Override
	protected void initialize() {
		super.addEzComponent(_mname);
		super.addEzComponent(_nPhotons);
		super.addEzComponent(_objNA);
		super.addEzComponent(_indexImmersion);
		super.addEzComponent(_lex);
		super.addEzComponent(_lem); 
		super.addEzComponent(_xySampling);
		super.addEzComponent(_zSampling);	
	}

	@Override
	protected void execute() {
		if(_lex.getValue()>_lem.getValue())
		{
			MessageDialog.showDialog("Emission wavelength should be greater than excitation wavelength (Stokes shift)", MessageDialog.ERROR_MESSAGE);
			return;
		}
		else{
			double sAlpha = _objNA.getValue()/_indexImmersion.getValue();
			if(sAlpha>1)
			{
				MessageDialog.showDialog("Effective NA available is lesser than the immersion medium refractive index. Assuming the effective NA to be" + _indexImmersion.getValue() + "?", MessageDialog.WARNING_MESSAGE);
				_objNA.setValue(_indexImmersion.getValue());
				sAlpha = 1.00;
			}

			double alpha = Math.asin(sAlpha);		
			double cAlpha = Math.cos(alpha);
			float xySampling = (float) (4 * _nPhotons.getValue() * _objNA.getValue() * sAlpha);
			float zSampling = (float) (2 * _nPhotons.getValue()  * _objNA.getValue() * (1-cAlpha));
			if(_mname.getValue() == "Wide-Field")
			{//WideField Calculations
				xySampling = _lem.getValue()/xySampling;
				zSampling = _lem.getValue()/zSampling;			
			}
			else
			{//Confocal
				xySampling = _lem.getValue()/(2 * xySampling);
				zSampling = _lem.getValue()/(2 * zSampling);	

			}

			_xySampling.setValue((double)xySampling);
			_zSampling.setValue((double)zSampling);

			//MessageDialog.INFORMATION_MESSAGE();
			//new AnnounceFrame("Radial Nyquist Sampling for the given " + _mname.getValue() + " Microscope is " + xySampling + " nm");
			//new AnnounceFrame("Axial Nyquist Sampling for the given " + _mname.getValue() + " Microscope is " + zSampling + " nm");
			//MessageDialog.showDialog("test is working fine !");
		}
	}

	@Override
	public void clean() {
		// TODO Auto-generated by Icy4Eclipse
	}
}
