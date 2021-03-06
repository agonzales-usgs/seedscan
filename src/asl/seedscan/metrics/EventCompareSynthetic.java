package asl.seedscan.metrics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sac.SacHeader;
import sac.SacTimeSeries;
import timeutils.MyFilter;
import asl.metadata.Channel;
import asl.metadata.meta_new.ChannelMeta.ResponseUnits;
import asl.metadata.meta_new.ChannelMetaException;
import asl.seedscan.event.EventCMT;
import asl.util.PlotMaker2;
import asl.util.PlotMakerException;
import asl.util.Trace;
import asl.util.TraceException;
import edu.sc.seis.TauP.SphericalCoords;

/**
 * <p>
 * The Class EventCompareSynthetic.
 * </p>
 * 
 * The difference is calculated by a power scale formula
 * result=SUM(data[i] * syn[i]) / SUM(syn[i] * syn[i])
 * <p>
 * Result meanings<br/>
 * x = 0  data is to small or all 0s. The channel is dead.<br/>
 * 0 < x < 1 data shows less displacement than the synthetic.<br/>
 * 1 = data aligns exactly with the synthetic.<br/>
 * x > 1 data shows greater displacement than the synthetic.<br/>
 * x < 0 The data is out of phase from the synthetic.<br/>
 * x = -1 The data is exactly 180 degrees out of phase, but matches the data.<br/>
 * </p>
 * 
 * @see <a
	 *      href="http://srl.geoscienceworld.org/content/77/1/12.full">Observations of Time-dependent Errors in Long-period Instrument Gain at Global Seismic Stations</a>
	 *      Equation 3 <br/>
 */
public class EventCompareSynthetic extends Metric {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.metrics.EventCompareSynthetic.class);

	/** The Constant PERIOD1. */
	private static final double PERIOD1 = 500;

	/** The Constant PERIOD2. */
	private static final double PERIOD2 = 400;

	/** The Constant PERIOD3. */
	private static final double PERIOD3 = 165;

	/** The Constant PERIOD4. */
	private static final double PERIOD4 = 85;

	/** The Constant f1. Period 1 frequency: 1./PERIOD1 */
	private static final double f1 = 1. / PERIOD1;

	/** The Constant f2. Period 2 frequency: 1./PERIOD2 */
	private static final double f2 = 1. / PERIOD2;

	/** The Constant f3. Period 3 frequency: 1./PERIOD3 */
	private static final double f3 = 1. / PERIOD3;

	/** The Constant f4. Period 4 frequency: 1./PERIOD4 */
	private static final double f4 = 1. / PERIOD4;

	/** The event CMTs. */
	private Hashtable<String, EventCMT> eventCMTs = null;

	/** The channels. */
	private Channel[] channels = null;

	/** The sac header. */
	private SacHeader hdr = null;

	/**
	 * @see asl.seedscan.metrics.Metric#getVersion()
	 */
	@Override
	public long getVersion() {
		return 2;
	}

	/**
	 * @see asl.seedscan.metrics.Metric#getName()
	 */
	@Override
	public String getName() {
		return "EventCompareSynthetic";
	}

	/**
	 * @see asl.seedscan.metrics.Metric#process()
	 */
	public void process() {
		logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

		eventCMTs = getEventTable();
		if (eventCMTs == null) {
			logger.info(String
					.format("No Event CMTs found for Day=[%s] --> Skip EventCompareSynthetic Metric",
							getDay()));
			return;
		}

		boolean compute00 = weHaveChannels("00", "LH");
		boolean compute10 = weHaveChannels("10", "LH");

		/**
		 * iDigest/ iMetric ChannelX v. ChannelY 0 channels[0] = 00-LHZ v.
		 * channels[6] = LXZ.modes.sac 1 channels[1] = 00-LHND v. channels[7] =
		 * LXN.modes.sac 2 channels[2] = 00-LHED v. channels[8] = LXE.modes.sac
		 * 3 channels[3] = 10-LHZ v. channels[6] = LXZ.modes.sac 4 channels[4] =
		 * 10-LHND v. channels[7] = LXN.modes.sac 5 channels[5] = 10-LHED v.
		 * channels[8] = LXE.modes.sac
		 **/
		int nChannels = 9;
		int nDigests = 6;

		ByteBuffer[] digestArray = new ByteBuffer[nDigests];
		// Channel[] channels = new Channel[nChannels];
		channels = new Channel[nChannels];

		double[] results = new double[nDigests];

		channels[0] = new Channel("00", "LHZ");
		channels[1] = new Channel("00", "LHND");
		channels[2] = new Channel("00", "LHED");
		channels[3] = new Channel("10", "LHZ");
		channels[4] = new Channel("10", "LHND");
		channels[5] = new Channel("10", "LHED");
		channels[6] = new Channel("XX", "LHZ");
		channels[7] = new Channel("XX", "LHN");
		channels[8] = new Channel("XX", "LHE");

		// Probably won't use channelY = synth in the metric identifier

		if (compute00) {
			for (int i = 0; i < 3; i++) {
				Channel channelX = channels[i];
				digestArray[i] = metricData.valueDigestChanged(channelX,
						createIdentifier(channelX), getForceUpdate());
				results[i] = 0.;
			}
		}
		if (compute10) {
			for (int i = 3; i < 6; i++) {
				Channel channelX = channels[i];
				digestArray[i] = metricData.valueDigestChanged(channelX,
						createIdentifier(channelX), getForceUpdate());
				results[i] = 0.;
			}
		}

		if (compute00) {
			if (digestArray[0] == null && digestArray[1] == null
					&& digestArray[2] == null) {
				compute00 = false;
			}
		}
		if (compute10) {
			if (digestArray[3] == null && digestArray[4] == null
					&& digestArray[5] == null) {
				compute10 = false;
			}
		}

		if (!compute00 && !compute10) {
			logger.info(String
					.format("== Day=[%s] Stn=[%s] - digest==null (or missing)for BOTH 00-LH and 10-LH chans --> Skip Metric",
							getDay(), getStation()));
			return;
		}

		// This is a little wonky: For instance, the digest for channel 00-LHND
		// will be computed using only metadata + data
		// for this channel (i.e., it will not explicitly include 00-LHED) and
		// for the current day
		// (i.e., if an event window extends into the next day, those data will
		// not form part of
		// the digest. Also, valueDigestChanged --> checkForRotatedChannels -->
		// createRotatedChannelData
		// will create rotated data + metadata for both horizontals (e.g.,
		// 00-LHND, 00-LHED) but
		// how do we get the rotated data for the next day ?

		int nEvents = 0;
		int eventNumber = 0;

		// Loop over Events for this day
		try {
			SortedSet<String> eventKeys = new TreeSet<String>(
					eventCMTs.keySet());
			for (String key : eventKeys) {
				Hashtable<String, SacTimeSeries> synthetics = getEventSynthetics(key);
				if (synthetics == null) {
					System.out
							.format("== %s: No synthetics found for key=[%s] for this station\n",
									getName(), key);
					continue;
				}
				// We do have synthetics for this station for this event -->
				// Compare to data
				// 1. Load up 3-comp synthetics
				SacTimeSeries[] sacSynthetics = new SacTimeSeries[3];
				String[] kcmp = { "Z", "N", "E" };
				for (int i = 0; i < 3; i++) {
					String fileKey = getStn() + ".XX.LX" + kcmp[i]
							+ ".modes.sac.proc"; // e.g.,
					// "ANMO.XX.LXZ.modes.sac.proc"
					if (synthetics.containsKey(fileKey)) {
						sacSynthetics[i] = synthetics.get(fileKey);
						MyFilter.bandpass(sacSynthetics[i], f1, f2, f3, f4);
					} else {
						logger.warn(String.format(
								"Did not find sac synthetic=[%s] in Hashtable",
								fileKey));
						return;
					}
				}
				/**
				 * try { //sacSynthetics[0].write("synth.z"); } catch (Exception
				 * e){ }
				 **/
				hdr = sacSynthetics[0].getHeader();

				eventNumber++;

				long eventStartTime = getSacStartTimeInMillis(hdr);
				// long eventStartTime = (eventCMTs.get(key)).getTimeInMillis();
				long duration = 8000000L; // 8000 sec = 8000000 msecs
				long eventEndTime = eventStartTime + duration;

				// 2. Load up Displacement Array
				ResponseUnits units = ResponseUnits.DISPLACEMENT;
				ArrayList<double[]> dataDisp = new ArrayList<double[]>();

				ArrayList<double[]> dataDisp00 = null;
				if (compute00) {
					dataDisp00 = metricData.getZNE(units, "00", "LH",
							eventStartTime, eventEndTime, f1, f2, f3, f4);
					if (dataDisp00 != null) {
						dataDisp.addAll(dataDisp00);
					} else {
						compute00 = false;
					}
				}
				ArrayList<double[]> dataDisp10 = null;
				if (compute10) {
					dataDisp10 = metricData.getZNE(units, "10", "LH",
							eventStartTime, eventEndTime, f1, f2, f3, f4);
					if (dataDisp10 != null) {
						dataDisp.addAll(dataDisp10);
					} else {
						compute10 = false;
					}
				}
				ArrayList<double[]> dataDisp3 = sacArrayToDouble(sacSynthetics);
				if (dataDisp3 == null) {
					logger.warn(
							"== {}: Error loading sac synthetics for stn=[{}] day=[{}] --> skip\n",
							getName(), getStation(), getDay());
					continue;
				} else {
					dataDisp.addAll(dataDisp3);
				}

				if (dataDisp00 == null && dataDisp10 == null) {
					System.out
							.format("== %s: getZNE returned null data --> skip this event\n",
									getName());
					continue;
				}

				// Window to use for comparisons
				int nstart = 0;
				int nend = hdr.getNpts() - 1;

				if (getMakePlots()) {
					makePlots(dataDisp00, dataDisp10, dataDisp3, nstart, nend,
							key, eventNumber);
				}

				if (compute00) {
					for (int i = 0; i < 3; i++) {
						// results[i] += 1.e6 * rmsDiff( dataDisp00.get(i),
						// dataDisp3.get(i), nstart, nend);
						results[i] += calcDiff(dataDisp00.get(i),
								dataDisp3.get(i), nstart, nend);
					}
				}
				if (compute10) {
					for (int i = 0; i < 3; i++) {
						// results[i+3] += 1.e6 * rmsDiff( dataDisp10.get(i),
						// dataDisp3.get(i), nstart, nend);
						results[i + 3] += calcDiff(dataDisp10.get(i),
								dataDisp3.get(i), nstart, nend);
					}
				}

				nEvents++;

			} // eventKeys: end loop over events

			if (nEvents == 0) { // Didn't make any measurements for this station
				return;
			}

			if (compute00) {
				for (int i = 0; i < 3; i++) {
					Channel channelX = channels[i];
					double result = results[i] / (double) nEvents;
					ByteBuffer digest = digestArray[i];
					metricResult.addResult(channelX, result, digest);
				}
			}
			if (compute10) {
				for (int i = 3; i < 6; i++) {
					Channel channelX = channels[i];
					double result = results[i] / (double) nEvents;
					ByteBuffer digest = digestArray[i];
					metricResult.addResult(channelX, result, digest);
				}
			}
		} catch (ChannelMetaException e) {
			logger.error("ChannelMetaException:", e);
		} catch (MetricException e) {
			logger.error("MetricException:", e);
		} catch (PlotMakerException e) {
			logger.error("PlotMakerException:", e);
		} catch (TraceException e) {
			logger.error("TraceException:", e);
		}
	} // end process()

	/**
	 * Gets the sac start time in millis.
	 * 
	 * @param hdr
	 *            the sac header
	 * @return the sac start time in millis
	 */
	private long getSacStartTimeInMillis(SacHeader hdr) {
		GregorianCalendar gcal = new GregorianCalendar(
				TimeZone.getTimeZone("GMT"));
		gcal.set(Calendar.YEAR, hdr.getNzyear());
		gcal.set(Calendar.DAY_OF_YEAR, hdr.getNzjday());
		gcal.set(Calendar.HOUR_OF_DAY, hdr.getNzhour());
		gcal.set(Calendar.MINUTE, hdr.getNzmin());
		gcal.set(Calendar.SECOND, hdr.getNzsec());
		gcal.set(Calendar.MILLISECOND, hdr.getNzmsec());

		return gcal.getTimeInMillis();
	}

	/**
	 * Sac array to double.
	 * 
	 * @param sacArray
	 *            the sac array
	 * @return the array list
	 */
	private ArrayList<double[]> sacArrayToDouble(SacTimeSeries[] sacArray) {
		ArrayList<double[]> sacDouble = new ArrayList<double[]>();
		for (int i = 0; i < sacArray.length; i++) {
			SacTimeSeries sac = sacArray[i];
			float[] fdata = sac.getY();
			double[] data = new double[fdata.length];
			for (int k = 0; k < fdata.length; k++) {
				data[k] = (double) fdata[k];
			}
			sacDouble.add(data);
		}

		return sacDouble;
	}

	/**
	 * Compare 2 double[] arrays between array indices n1 and n2 currently
	 * doing: SUM[ x(n) * y(n) ] / SUM[ y(n) * y(n) ], where x(n)=data and
	 * y(n)=synth
	 * 
	 * difference = 0. --> data are all zero<br/>
	 * difference = 1. --> data exactly matches synthetic<br/>
	 * difference = 1. --> data exactly matches -synthetic (is 180 deg out of
	 * phase)<br/>
	 * 
	 * @see <a
	 *      href="http://srl.geoscienceworld.org/content/77/1/12.full">http://srl.geoscienceworld.org/content/77/1/12.full</a>
	 *      Equation 3 <br/>
	 * 
	 * 
	 *      data1 = x, data2 = y
	 * 
	 * @param data1
	 *            the data in meters displaced
	 * @param data2
	 *            the synthetic in meters displaced
	 * @param n1
	 *            the window start; nstart in process()
	 * @param n2
	 *            the window end; nend in process()
	 * @return the result
	 */
	private double calcDiff(double[] data1, double[] data2, int n1, int n2) {
		if (n2 < n1) {
			logger.error(
					"station=[{}] day=[{}]: calcDiff: n2 < n1 --> Bad window",
					getStation(), getDay());
			return NO_RESULT;
		}
		if (n2 >= data1.length || n2 >= data2.length) {
			logger.error(
					"station=[{}] day=[{}]: calcDiff: n2=[{}] > data1.length=[{}] and/or data2.length=[{}] --> Bad window",
					getStation(), getDay(), n2, data1.length, data2.length);
			return NO_RESULT;
		}
		double numerator = 0.;
		double denomenator = 0.;

		for (int i = n1; i < n2; i++) {
			numerator += data1[i] * data2[i];
			denomenator += data2[i] * data2[i];
		}

		if (denomenator == 0.) {
			logger.error(
					"station=[{}] day=[{}]: calcDiff: denomenator==0 --> Divide by 0 --> Expect result = Infinity!",
					getStation(), getDay());
		}
		double result = numerator / denomenator;

		return result;
	}

	/**
	 * Make plots.
	 * 
	 * @param d00
	 *            the 00 data
	 * @param d10
	 *            the 10 data
	 * @param d20
	 *            the 20 data
	 * @param nstart
	 *            the window start; nstart in process()
	 * @param nend
	 *            the window end; nend in process()
	 * @param key
	 *            the key
	 * @param eventNumber
	 *            the event number
	 * @throws PlotMakerException
	 *             the plot maker exception
	 * @throws TraceException
	 *             the trace exception
	 */
	public void makePlots(ArrayList<double[]> d00, ArrayList<double[]> d10,
			ArrayList<double[]> d20, int nstart, int nend, String key,
			int eventNumber) throws PlotMakerException, TraceException {
		PlotMaker2 plotMaker = null;
		EventCMT eventCMT = eventCMTs.get(key);
		double evla = eventCMT.getLatitude();
		double evlo = eventCMT.getLongitude();
		double stla = stationMeta.getLatitude();
		double stlo = stationMeta.getLongitude();
		double gcarc = SphericalCoords.distance(evla, evlo, stla, stlo);

		final String plotTitle = String.format(
				"[ Event: %s ] [ Station: %s ] [ Dist: %.2f ] %s", key,
				getStation(), gcarc, getName());

		final String pngName = String.format("%s.synthcompare.ev-%d.png",
				getOutputDir(), eventNumber);

		if (plotMaker == null) {
			plotMaker = new PlotMaker2(plotTitle);
			plotMaker.initialize3Panels("LHZ", "LH1/LHN", "LH2/LHE");
		}

		BasicStroke stroke = new BasicStroke(2.0f);

		int npts = nend - nstart + 1;

		double[] xsecs = new double[npts];
		for (int k = 0; k < xsecs.length; k++) {
			xsecs[k] = (float) (k + nstart); // hard-wired for LH? dt=1.0
		}

		try { // addTraceToPanel()
			if (d00 != null) {
				for (int i = 0; i < d00.size(); i++) {
					double[] dataIn = d00.get(i);
					double[] dataOut = new double[npts];
					System.arraycopy(dataIn, nstart, dataOut, 0, npts);
					plotMaker.addTraceToPanel(new Trace(xsecs, dataOut,
							channels[i].toString(), Color.green, stroke), i);
				}
			}
			if (d10 != null) {
				for (int i = 0; i < d10.size(); i++) {
					double[] dataIn = d10.get(i);
					double[] dataOut = new double[npts];
					System.arraycopy(dataIn, nstart, dataOut, 0, npts);
					plotMaker.addTraceToPanel(new Trace(xsecs, dataOut,
							channels[i + 3].toString(), Color.red, stroke), i);
				}
			}
			if (d20 != null) {
				for (int i = 0; i < d20.size(); i++) {
					double[] dataIn = d20.get(i);
					double[] dataOut = new double[npts];
					System.arraycopy(dataIn, nstart, dataOut, 0, npts);
					// Convert synthetic channel XX-LHN to XX-LHND for plot
					// legend
					String kChan = channels[i + 6].toString();
					if (channels[i + 6].toString().contains("LHN")
							|| channels[i + 6].toString().contains("LHE")) {
						kChan += "D";
					}
					plotMaker.addTraceToPanel(new Trace(xsecs, dataOut, kChan,
							Color.black, stroke), i);
				}
			}

			plotMaker.writePlot(pngName);
		} catch (PlotMakerException e) {
			throw e;
		} catch (TraceException e) {
			throw e;
		}
	}
}