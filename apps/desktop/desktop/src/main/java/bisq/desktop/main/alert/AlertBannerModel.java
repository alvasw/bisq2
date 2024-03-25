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

package bisq.desktop.main.alert;

import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.desktop.common.view.Model;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class AlertBannerModel implements Model {
    private final ObservableList<AuthorizedAlertData> observableList = FXCollections.observableArrayList();
    private final SortedList<AuthorizedAlertData> sortedList = new SortedList<>(observableList);

    @Setter
    private AuthorizedAlertData displayedAuthorizedAlertData;
    private final BooleanProperty isAlertVisible = new SimpleBooleanProperty();
    private final StringProperty headline = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();
    private final ObjectProperty<AlertType> alertType = new SimpleObjectProperty<>();

    public AlertBannerModel() {
    }

    void reset() {
        displayedAuthorizedAlertData = null;
        isAlertVisible.set(false);
        headline.set(null);
        message.set(null);
        alertType.set(null);
    }
}
