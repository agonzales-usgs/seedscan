/*
 * Copyright 2012, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */
package asl.seedscan.metrics;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.PolynomialStage;
import asl.metadata.meta_new.ResponseStage;
import asl.seedsplitter.DataSet;

public class MassPositionMetric extends Metric {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.metrics.MassPositionMetric.class);

	@Override
	public long getVersion() {
		return 1;
	}

	@Override
	public String getName() {
		return "MassPositionMetric";
	}

	public void process() {
		logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

		String station = getStation();
		String day = getDay();
		String metric = getName();

		// Get all VM? channels in metadata to use for loop
		List<Channel> channels = stationMeta.getChannelArray("VM");

		// Loop over channels, get metadata & data for channel and Calculate
		// Metric
		for (Channel channel : channels) {
			if (!metricData.hasChannelData(channel)) {
				logger.warn(
						"No data found for channel:[{}] day:[{}] --> Skip metric",
						channel, day);
				continue;
			}

			ByteBuffer digest = metricData.valueDigestChanged(channel,
					createIdentifier(channel), getForceUpdate());

			if (digest == null) { // means oldDigest == newDigest and we don't
				// need to recompute the metric
				logger.warn(
						"Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
						getStation(), channel, day);
				continue;
			}

			try {
				double result = computeMetric(channel, station, day, metric);

				metricResult.addResult(channel, result, digest);
			} catch (MetricException e) {
				logger.error("Exception:", e);
			}

		}// end foreach channel
	} // end process()

	private double computeMetric(Channel channel, String station, String day,
			String metric) throws MetricException {
		ChannelMeta chanMeta = stationMeta.getChanMeta(channel);
		List<DataSet> datasets = metricData.getChannelData(channel);

		double a0 = 0;
		double a1 = 0;
		double upperBound = 0;
		double lowerBound = 0;

		// Get Stage 1, make sure it is a Polynomial Stage (MacLaurin) and get
		// Coefficients
		// will add RuntimeException() to logger.error('msg', e)
		ResponseStage stage = chanMeta.getStage(1);
		if (!(stage instanceof PolynomialStage)) {
			StringBuilder message = new StringBuilder();
			message.append(String
					.format("station=[%s] channel=[%s] day=[%s]: Stage 1 is NOT a PolynomialStage!\n",
							station, channel.toString(), day));
			throw new MetricException(message.toString());
		}
		PolynomialStage polyStage = (PolynomialStage) stage;
		double[] coefficients = polyStage.getRealPolynomialCoefficients();
		lowerBound = polyStage.getLowerApproximationBound();
		upperBound = polyStage.getUpperApproximationBound();

		// We're expecting a MacLaurin Polynomial with 2 coefficients (a0, a1)
		// to represent mass position
		if (coefficients.length != 2) {
			StringBuilder message = new StringBuilder();
			message.append(String
					.format("station=[%s] channel=[%s] day=[%s]: We're expecting 2 coefficients for this PolynomialStage!\n",
							station, channel.toString(), day));
			throw new MetricException(message.toString());
		} else {
			a0 = coefficients[0];
			a1 = coefficients[1];
		}
		// Make sure we have enough ingredients to calculate something useful
		if (a0 == 0 && a1 == 0 || lowerBound == 0 && upperBound == 0) {
			StringBuilder message = new StringBuilder();
			message.append(String
					.format("station=[%s] channel=[%s] day=[%s]: We don't have enough information to compute mass position!\n",
							station, channel.toString(), day));
			throw new MetricException(message.toString());
		}

		double massPosition = 0;
		int ndata = 0;

		for (DataSet dataset : datasets) {
			int intArray[] = dataset.getSeries();
			for (int j = 0; j < intArray.length; j++) {
				massPosition += Math.pow((a0 + intArray[j] * a1), 2);
			}
			ndata += dataset.getLength();
		} // end for each dataset

		massPosition = Math.sqrt(massPosition / (double) ndata);

		double massRange = (upperBound - lowerBound) / 2;
		double massCenter = lowerBound + massRange;

		return 100. * Math.abs(massPosition - massCenter) / massRange;
	}
} // end class
