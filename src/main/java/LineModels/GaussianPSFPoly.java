package LineModels;

public class GaussianPSFPoly implements MTFitFunction {

	@Override
	public double val(double[] x, double[] a, double[] b) {
		final int ndims = x.length;
		
		
		return  a[2 * ndims + 1] * Etotal(x, a, b) + a[2 * ndims + 2] ;
		
	}

	@Override
	public double grad(double[] x, double[] a, double[] b, int k) {
		final int ndims = x.length;

		if (k < ndims) {

			return 2 * a[k + 2 * x.length + 3]  * (x[k] - a[k])  * a[2 * ndims + 1] * Estart(x, a, b);

		}

		else if (k >= ndims && k <= ndims + 1) {
			int dim = k - ndims;
			return 2 *  a[dim + 2 * x.length + 3]  * (x[dim] - a[k])  * a[2 * ndims + 1] * Eend(x, a, b);

		}

		else if (k == 2 * ndims)
			return  a[2 * ndims + 1] *Estartds(x, a, b);
		
		
		else if (k == 2 * ndims + 1)
			return Etotal(x, a, b);
		
		
		else if (k == 2 * ndims + 2)
			return 1.0;

		else if (k == 2 * ndims + 3)
			return a[2 * ndims + 1] * EsigmaX(x, a, b);
		
		
		else if (k == 2 * ndims + 4)
			return a[2 * ndims + 1] * EsigmaY(x, a, b);
	/*	
		else if (k > 2 * ndims + 2 && k < 2 * ndims + 5){
			
			
			// With respect to ai (sigma)
						int dim = k - 2 * ndims - 3;
						double di = x[dim] - a[dim];
						double dii = x[dim] - a[dim + ndims];
						return - di * di * a[2 * ndims + 1] * Estart(x, a, b)
								- dii * dii * a[2 * ndims + 1] * Eend(x, a, b);
			
		}
		*/
		else
			return 0;

	}

	/*
	 * PRIVATE METHODS
	 */

	/*
	 * @ Define a line analytically as a sum of gaussians, the parameters to be
	 * determined are the start and the end points of the line
	 * 
	 */

	private static final double EsigmaX(final double[] x, final double[] a, final double[] b){
		
		double sum = 0, dsum = 0, sumofgaussians = 0;
		double di;
		final int ndims = x.length;
		double[] minVal = new double[ndims];
		double[] maxVal = new double[ndims];

		for (int i = 0; i < x.length; i++) {
			minVal[i] = a[i];
			maxVal[i] = a[ndims + i];
		}
	    double slope = (maxVal[1] - minVal[1]) / (maxVal[0] - minVal[0]);
		
		
		double ds = Math.abs(a[2 * ndims]);

		double[] dxvector = { ds / Math.sqrt( 1 + slope * slope) , slope * ds/ Math.sqrt( 1 + slope * slope)  };
		
		while (true) {
		
		for (int i = 0; i < x.length; i++) {
			minVal[i] += dxvector[i];
			di = x[0] - minVal[0];
			dsum += -di * di ;
			sum += a[i + 2 * x.length + 3] * di * di;
			
		}
		
		sumofgaussians+= dsum * Math.exp(-sum);
		
		if (minVal[0] >= maxVal[0] || minVal[1] >= maxVal[1] && slope > 0)
			break;
		if (minVal[0] >= maxVal[0] || minVal[1] <= maxVal[1] && slope < 0)
			break;
		
		}
		
		
		return sumofgaussians;
		
	}
	
	
private static final double EsigmaY(final double[] x, final double[] a, final double[] b){
		
		double sum = 0, dsum = 0, sumofgaussians = 0;
		double di;
		final int ndims = x.length;
		double[] minVal = new double[ndims];
		double[] maxVal = new double[ndims];

		for (int i = 0; i < x.length; i++) {
			minVal[i] = a[i];
			maxVal[i] = a[ndims + i];
		}
	    double slope = (maxVal[1] - minVal[1]) / (maxVal[0] - minVal[0]);
		
		
		double ds = Math.abs(a[2 * ndims]);

		double[] dxvector = { ds / Math.sqrt( 1 + slope * slope) , slope * ds/ Math.sqrt( 1 + slope * slope)  };
		
		while (true) {
		
		for (int i = 0; i < x.length; i++) {
			minVal[i] += dxvector[i];
			di = x[1] - minVal[1];
			dsum += -di * di ;
			sum += a[i + 2 * x.length + 3] * di * di;
			
		}
		
		sumofgaussians+= dsum * Math.exp(-sum);
		
		if (minVal[0] >= maxVal[0] || minVal[1] >= maxVal[1] && slope > 0)
			break;
		if (minVal[0] >= maxVal[0] || minVal[1] <= maxVal[1] && slope < 0)
			break;
		
		}
		
		
		return sumofgaussians;
		
	}
	
	private static final double Estart(final double[] x, final double[] a, final double[] b) {

		double sum = 0;
		double di;
		for (int i = 0; i < x.length; i++) {
			di = x[i] - a[i];
			sum += a[i + 2 * x.length + 3] * di * di;
		}

		return Math.exp(-sum);

	}

	private static final double Estartds(final double[] x, final double[] a, final double[] b) {

		
		double di;
		final int ndims = x.length;
		double[] minVal = new double[ndims];
		double[] maxVal = new double[ndims];

		for (int i = 0; i < x.length; i++) {
			minVal[i] = a[i];
			maxVal[i] = a[ndims + i];
		}
		double slope = (maxVal[1] - minVal[1]) / (maxVal[0] - minVal[0]);
		
		
		double ds = Math.abs(a[2 * ndims]);

		double[] dxvector = { ds / Math.sqrt( 1 + slope * slope) , slope * ds/ Math.sqrt( 1 + slope * slope)  };
		double[] dxvectorderiv = { 1/ Math.sqrt( 1 + slope * slope) , slope/ Math.sqrt( 1 + slope * slope)  };

		
		
		double dsum = 0;
		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			minVal[i] += dxvector[i];
			di = x[i] - minVal[i];
			sum += a[i + 2 * x.length + 3] * di * di;
			dsum += 2 * a[i + 2 * x.length + 3]  * di * dxvectorderiv[i];
		}
		double sumofgaussians = dsum * Math.exp(-sum);
		
		double dsumend = 0;
		double sumend = 0;
		for (int i = 0; i < x.length; i++) {
			maxVal[i] -= dxvector[i];
			di = x[i] - maxVal[i];
			sumend += a[i + 2 * x.length + 3] * di * di;
			dsumend += -2 * a[i + 2 * x.length + 3]  * di * dxvectorderiv[i];
		}
		sumofgaussians+= dsumend * Math.exp(-sumend);
		
		
		return   sumofgaussians ;

	}

	
	private static final double Eend(final double[] x, final double[] a, final double[] b) {

		double sum = 0;
		double di;
		int ndims = x.length;
		for (int i = 0; i < x.length; i++) {
			di = x[i] - a[i + ndims];
			sum += a[i + 2 * x.length + 3]  * di * di;
		}

		return Math.exp(-sum);

	}

	private static final double Etotal(final double[] x, final double[] a, final double[] b) {

		return Estart(x, a, b) + Esum(x, a, b) + Eend(x, a, b);

	}

	private static final double Esum(final double[] x, final double[] a, final double[] b) {

		final int ndims = x.length;
		double[] minVal = new double[ndims];
		double[] maxVal = new double[ndims];

		for (int i = 0; i < x.length; i++) {
			minVal[i] = a[i];
			maxVal[i] = a[ndims + i];
		}
		double slope = (maxVal[1] - minVal[1]) / (maxVal[0] - minVal[0]);
		double sum = 0;
		double sumofgaussians = 0;
		double di;
		
		
		double ds = Math.abs(a[2 * ndims]);

		double[] dxvector = { ds/ Math.sqrt( 1 + slope * slope) , slope * ds/ Math.sqrt( 1 + slope * slope)  };

		while (true) {

			sum = 0;
			for (int i = 0; i < x.length; i++) {
				minVal[i] += dxvector[i];
				di = x[i] - minVal[i];
				sum += a[i + 2 * x.length + 3]  * di * di;
			}
			sumofgaussians += Math.exp(-sum);

			
			if (minVal[0] >= maxVal[0] || minVal[1] >= maxVal[1] && slope > 0)
				break;
			if (minVal[0] >= maxVal[0] || minVal[1] <= maxVal[1] && slope < 0)
				break;

		}
		
		
		

		return sumofgaussians;
	}

	public static double Distance(final double[] cordone, final double[] cordtwo) {

		double distance = 0;
		final double ndims = cordone.length;

		for (int d = 0; d < ndims; ++d) {

			distance += Math.pow((cordone[d] - cordtwo[d]), 2);

		}
		return Math.sqrt(distance);
	}

}