/*
 * This file is part of DeltaRedis.
 *
 * DeltaRedis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaRedis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaRedis.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.deltaredis.shared.structure;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/22/16.
 */
public interface Shutdownable {
    /**
     * This method clears all internal, owned data structures, nullifies references, and
     * performs all functions necessary to cleanup the object making it unusable after
     * this call.
     */
    void shutdown();
}
