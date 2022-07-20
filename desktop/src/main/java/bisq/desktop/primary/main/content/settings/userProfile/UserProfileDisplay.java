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

package bisq.desktop.primary.main.content.settings.userProfile;

import bisq.common.data.Pair;
import bisq.common.data.Triple;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UserProfileDisplay {
    private final Controller controller;

    public UserProfileDisplay(UserIdentityService userIdentityService, UserIdentity userIdentity) {
        controller = new Controller(userIdentityService, userIdentity);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final UserIdentityService userIdentityService;
        private final Model model;
        @Getter
        private final View view;


        private Controller(UserIdentityService userIdentityService, UserIdentity userIdentity) {
            this.userIdentityService = userIdentityService;
            model = new Model(userIdentity);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            UserProfile userProfile = model.userIdentity.getUserProfile();

            model.id.set(Res.get("social.createUserProfile.id", userProfile.getId()));
            model.bio.set(userProfile.getStatement());
            model.terms.set(userProfile.getTerms());
            model.reputationScore.set(userProfile.getBurnScoreAsString());
            model.profileAge.set(userProfile.getAccountAgeAsString());

            model.nym.set(userProfile.getNym());
            model.nickName.set(userProfile.getNickName());
            model.roboHashNode.set(RoboHash.getImage(userProfile.getPubKeyHash()));
        }

        @Override
        public void onDeactivate() {
        }

        @Override
        public boolean useCaching() {
            return false;
        }

        public void onEdit() {
            Navigation.navigateTo(NavigationTarget.EDIT_PROFILE);
        }

        public void onCancelEdit() {
            model.isEditMode.set(false);
        }

        public void onSave(String terms, String bio) {
            model.isPublishing.set(true);
            model.progress.set(-1);
            userIdentityService.editUserProfile(model.userIdentity, terms, bio)
                    .whenComplete((r, t) -> {
                        model.progress.set(0);
                        model.isPublishing.set(false);
                        model.isEditMode.set(false);
                    });
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final UserIdentity userIdentity;
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty nym = new SimpleStringProperty();
        private final StringProperty nickName = new SimpleStringProperty();
        private final StringProperty id = new SimpleStringProperty();
        private final StringProperty bio = new SimpleStringProperty();
        private final StringProperty terms = new SimpleStringProperty();
        private final StringProperty reputationScore = new SimpleStringProperty();
        private final StringProperty profileAge = new SimpleStringProperty();
        private final BooleanProperty isEditMode = new SimpleBooleanProperty();
        private final BooleanProperty isPublishing = new SimpleBooleanProperty();
        private final IntegerProperty progress = new SimpleIntegerProperty();

        private Model(UserIdentity userIdentity) {
            this.userIdentity = userIdentity;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Label nym, nickName, bio, reputationScore, profileAge, terms;
        private final BisqTextArea bioTextArea, termsTextArea;
        private final Button editButton, saveButton, cancelEditButton;
        private final HBox buttonBar;
        private final ProgressIndicator progressIndicator;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            root.setAlignment(Pos.TOP_LEFT);

            Label headlineLabel = new Label(Res.get("settings.userProfile.selectedProfile").toUpperCase());
            headlineLabel.getStyleClass().add("bisq-text-4");

            nickName = new Label();
            nickName.getStyleClass().addAll("bisq-text-9", "font-semi-bold");
            nickName.setAlignment(Pos.CENTER);

            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(100);
            roboIconImageView.setFitHeight(100);

            nym = new Label();
            nym.getStyleClass().addAll("bisq-text-7");
            nym.setAlignment(Pos.CENTER);

            VBox nameAndIconBox = new VBox(10, nickName, roboIconImageView, nym);
            nameAndIconBox.setAlignment(Pos.TOP_CENTER);

            Triple<VBox, Label, BisqTextArea> bioBox = getEditableInfoBox(Res.get("social.chatUser.statement"));
            bio = bioBox.getSecond();
            bioTextArea = bioBox.getThird();

            Pair<VBox, Label> reputationScoreBox = getInfoBox(Res.get("social.chatUser.reputationScore"));
            reputationScore = reputationScoreBox.getSecond();

            Pair<VBox, Label> profileAgeBox = getInfoBox(Res.get("social.chatUser.profileAge"));
            profileAge = profileAgeBox.getSecond();

            Triple<VBox, Label, BisqTextArea> termsBox = getEditableInfoBox(Res.get("social.chat.terms.headline"));
            terms = termsBox.getSecond();
            termsTextArea = termsBox.getThird();

            editButton = new Button(Res.get("edit"));

            saveButton = new Button(Res.get("save"));
            saveButton.setDefaultButton(true);

            cancelEditButton = new Button(Res.get("cancel"));

            progressIndicator = new ProgressIndicator(0);
            progressIndicator.setManaged(false);
            progressIndicator.setVisible(false);
            buttonBar = new HBox(10, cancelEditButton, saveButton, progressIndicator, Spacer.fillHBox());
            buttonBar.setAlignment(Pos.CENTER_LEFT);
            buttonBar.setManaged(false);
            buttonBar.setVisible(false);


            VBox.setMargin(nameAndIconBox, new Insets(0, 0, 20, 0));
            VBox.setMargin(editButton, new Insets(20, 0, 0, 0));
            VBox.setMargin(buttonBar, new Insets(20, 0, 0, 0));
            VBox mainVBox = new VBox(10, headlineLabel, nameAndIconBox,
                    bioBox.getFirst(), reputationScoreBox.getFirst(), profileAgeBox.getFirst(), termsBox.getFirst(), editButton, buttonBar);
            mainVBox.getStyleClass().add("bisq-box-2");
            mainVBox.setPadding(new Insets(30));

            VBox.setMargin(headlineLabel, new Insets(0, 0, 0, 0));

            root.getChildren().addAll(headlineLabel, mainVBox);
        }

        @Override
        protected void onViewAttached() {
            nym.textProperty().bind(model.nym);
            nickName.textProperty().bind(model.nickName);
            bio.textProperty().bind(model.bio);
            terms.textProperty().bind(model.terms);
            bioTextArea.textProperty().bindBidirectional(model.bio);
            termsTextArea.textProperty().bindBidirectional(model.terms);
            reputationScore.textProperty().bind(model.reputationScore);
            profileAge.textProperty().bind(model.profileAge);

            bio.managedProperty().bind(model.isEditMode.not());
            bio.visibleProperty().bind(model.isEditMode.not());
            terms.managedProperty().bind(model.isEditMode.not());
            terms.visibleProperty().bind(model.isEditMode.not());

            bioTextArea.managedProperty().bind(model.isEditMode);
            bioTextArea.visibleProperty().bind(model.isEditMode);
            termsTextArea.managedProperty().bind(model.isEditMode);
            termsTextArea.visibleProperty().bind(model.isEditMode);

            editButton.managedProperty().bind(model.isEditMode.not());
            editButton.visibleProperty().bind(model.isEditMode.not());
            buttonBar.managedProperty().bind(model.isEditMode);
            buttonBar.visibleProperty().bind(model.isEditMode);
            progressIndicator.managedProperty().bind(model.isPublishing);
            progressIndicator.visibleProperty().bind(model.isPublishing);

            cancelEditButton.disableProperty().bind(model.isPublishing);
            saveButton.disableProperty().bind(model.isPublishing);
            progressIndicator.progressProperty().bind(model.progress);

            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    roboIconImageView.setImage(roboIcon);
                }
            });

            editButton.setOnAction(e -> controller.onEdit());
            cancelEditButton.setOnAction(e -> controller.onCancelEdit());
            saveButton.setOnAction(e -> controller.onSave(termsTextArea.getText(), bioTextArea.getText()));
        }

        @Override
        protected void onViewDetached() {
            nym.textProperty().unbind();
            nickName.textProperty().unbind();
            bio.textProperty().unbind();
            terms.textProperty().unbind();
            bioTextArea.textProperty().unbindBidirectional(model.bio);
            termsTextArea.textProperty().unbindBidirectional(model.terms);
            reputationScore.textProperty().unbind();
            profileAge.textProperty().unbind();

            bio.managedProperty().unbind();
            bio.visibleProperty().unbind();
            terms.managedProperty().unbind();
            terms.visibleProperty().unbind();

            bioTextArea.managedProperty().unbind();
            bioTextArea.visibleProperty().unbind();
            termsTextArea.managedProperty().unbind();
            termsTextArea.visibleProperty().unbind();

            editButton.managedProperty().unbind();
            editButton.visibleProperty().unbind();
            buttonBar.managedProperty().unbind();
            buttonBar.visibleProperty().unbind();
            progressIndicator.managedProperty().unbind();
            progressIndicator.visibleProperty().unbind();

            cancelEditButton.disableProperty().unbind();
            saveButton.disableProperty().unbind();
            progressIndicator.progressProperty().unbind();

            roboHashNodeSubscription.unsubscribe();

            editButton.setOnAction(null);
            cancelEditButton.setOnAction(null);
            saveButton.setOnAction(null);
        }

        private Pair<VBox, Label> getInfoBox(String title) {
            Label titleLabel = new Label(title.toUpperCase());
            titleLabel.getStyleClass().addAll("bisq-text-4", "bisq-text-grey-9", "font-semi-bold");

            Label contentLabel = new Label();
            contentLabel.getStyleClass().addAll("bisq-text-6", "wrap-text");

            VBox box = new VBox(2, titleLabel, contentLabel);
            VBox.setMargin(box, new Insets(2, 0, 0, 0));

            return new Pair<>(box, contentLabel);
        }

        private Triple<VBox, Label, BisqTextArea> getEditableInfoBox(String title) {
            Label titleLabel = new Label(title.toUpperCase());
            titleLabel.getStyleClass().addAll("bisq-text-4", "bisq-text-grey-9", "font-semi-bold");

            Label contentLabel = new Label();
            contentLabel.getStyleClass().addAll("bisq-text-6", "wrap-text");

            BisqTextArea editTextArea = new BisqTextArea();
            editTextArea.setManaged(false);
            editTextArea.setVisible(false);

            VBox box = new VBox(2, titleLabel, contentLabel, editTextArea);
            VBox.setMargin(box, new Insets(2, 0, 0, 0));

            return new Triple<>(box, contentLabel, editTextArea);
        }
    }
}