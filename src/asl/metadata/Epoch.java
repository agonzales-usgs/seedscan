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
package asl.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

public class Epoch
{
    private static final Logger logger = LoggerFactory.getLogger(asl.metadata.Epoch.class);

    public static final int STATION_EPOCH_BLOCKETTE_NUMBER   = 50;
    public static final int STATION_COMMENT_BLOCKETTE_NUMBER = 51;
    public static final int CHANNEL_EPOCH_BLOCKETTE_NUMBER   = 52;
    public static final int CHANNEL_COMMENT_BLOCKETTE_NUMBER = 59;

    private String startDate   = null;
    private String endDate     = null;
    private Calendar timestamp = null;

    // Constructors
    public Epoch(Blockette blockette)
    throws TimestampFormatException,
           WrongBlocketteException,
           MissingBlocketteDataException
    {
        if (blockette.getNumber() == STATION_EPOCH_BLOCKETTE_NUMBER) {
            startDate = blockette.getFieldValue(13, 0);
            endDate   = blockette.getFieldValue(14, 0);
        }
        else if (blockette.getNumber() == STATION_COMMENT_BLOCKETTE_NUMBER) {
            startDate = blockette.getFieldValue(3, 0);
            endDate   = blockette.getFieldValue(4, 0);
        }
        else if (blockette.getNumber() == CHANNEL_EPOCH_BLOCKETTE_NUMBER) {
            startDate = blockette.getFieldValue(22, 0);
            endDate   = blockette.getFieldValue(23, 0);
        }
        else if (blockette.getNumber() == CHANNEL_COMMENT_BLOCKETTE_NUMBER) {
            startDate = blockette.getFieldValue(3, 0);
            endDate   = blockette.getFieldValue(4, 0);
        }
        else {
        	WrongBlocketteException e = new WrongBlocketteException();
        	logger.error("Epoch WrongBlocketteException:", e);
            return;
        }
        if (startDate == null) {
        	MissingBlocketteDataException e = new MissingBlocketteDataException();
        	logger.error("Epoch MissingBlocketteDataException:", e);
            return;
        }
        this.timestamp = BlocketteTimestamp.parseTimestamp(startDate);
        System.out.format("Epoch(): [%s - %s] [%s:%s]\n",startDate,endDate,String.format("%1$tY", timestamp), String.format("%1$tj", timestamp) );
    }

}
