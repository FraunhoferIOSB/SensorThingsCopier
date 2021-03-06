/*
 * Copyright (C) 2016 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.stc;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class ObservationCopier {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationCopier.class);
    SensorThingsService sourceService;
    SensorThingsService targetService;
    private final DatastreamCombo datastreamCombo;
    /**
     * The time to sleep after inserting a new Observation.
     */
    private long delay = 1;
    /**
     * The number of observations to fetch per request.
     */
    private int perRequest = 1000;

    public ObservationCopier(SensorThingsService sourceService, SensorThingsService targetService, DatastreamCombo combo) {
        this.sourceService = sourceService;
        this.targetService = targetService;
        this.datastreamCombo = combo;
    }

    public synchronized long doWork() throws URISyntaxException, ServiceFailureException, MalformedURLException {
        LOGGER.debug("Copying {} to {}.", datastreamCombo.getSourceDatastreamId(), datastreamCombo.getTargetDatastreamId());

        Datastream sourceDatastream = sourceService.datastreams().find(datastreamCombo.getSourceDatastreamId());
        Datastream targetDatastream = targetService.datastreams().find(datastreamCombo.getTargetDatastreamId());

        EntityList<Observation> list = sourceDatastream
                .observations().query()
                .filter("id gt " + datastreamCombo.getLastCopiedId())
                .orderBy("id asc")
                .top(1000)
                .list();
        long count = 0;
        long message = 1000;
        Iterator<Observation> i = list.fullIterator();
        while (i.hasNext()) {
            Observation observation = i.next();
            Long sourceId = (Long) observation.getId().getValue();
            observation.setService(null);
            observation.setSelfLink((URI) null);
            observation.setId(null);
            targetDatastream.observations().create(observation);
            Long targetId = (Long) observation.getId().getValue();
            LOGGER.trace("Copied Obs {}. New Id: {}.", sourceId, targetId);
            datastreamCombo.setLastCopiedId(sourceId);
            count++;
            message--;
            if (message <= 0) {
                LOGGER.info("Copied {}...", count);
                message = 1000;
            }
            maybeSleep();
        }
        LOGGER.info("Copied {} observations from {} to {}. LastId={}.",
                count,
                datastreamCombo.getSourceDatastreamId(),
                datastreamCombo.getTargetDatastreamId(),
                datastreamCombo.getLastCopiedId());
        return datastreamCombo.getLastCopiedId();
    }

    private void maybeSleep() {
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                LOGGER.warn("Rude wakeop.", ex);
            }
        }
    }

    public DatastreamCombo getDatastreamCombo() {
        return datastreamCombo;
    }

    /**
     * The time to sleep after inserting a new Observation.
     *
     * @return the delay
     */
    public long getDelay() {
        return delay;
    }

    /**
     * The time to sleep after inserting a new Observation.
     *
     * @param delay the delay to set
     */
    public void setDelay(long delay) {
        this.delay = delay;
    }

    /**
     * The number of observations to fetch per request.
     *
     * @return the perRequest
     */
    public int getPerRequest() {
        return perRequest;
    }

    /**
     * The number of observations to fetch per request.
     *
     * @param perRequest the perRequest to set
     */
    public void setPerRequest(int perRequest) {
        this.perRequest = perRequest;
    }

}
