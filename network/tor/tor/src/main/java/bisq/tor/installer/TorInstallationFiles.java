/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.tor.installer;

import bisq.common.util.FileUtils;
import bisq.tor.Constants;
import bisq.tor.OsType;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Getter
public class TorInstallationFiles {
    private final File torDir;
    private final File torBinary;
    private final File pidFile;
    private final File geoIPFile;
    private final File geoIPv6File;
    private final File torrcFile;
    private final File versionFile;

    public TorInstallationFiles(Path torDirPath, OsType osType) {
        torDir = torDirPath.toFile();
        torBinary = new File(torDir, osType.getBinaryName());
        pidFile = new File(torDir, Constants.PID);
        geoIPFile = new File(torDir, Constants.GEO_IP);
        geoIPv6File = new File(torDir, Constants.GEO_IPV_6);
        torrcFile = new File(torDir, Constants.TORRC);
        versionFile = new File(torDir, Constants.VERSION);
    }

    public void writePidToDisk(String ownerPid) throws IOException {
        FileUtils.writeToFile(ownerPid, pidFile);
    }
}
