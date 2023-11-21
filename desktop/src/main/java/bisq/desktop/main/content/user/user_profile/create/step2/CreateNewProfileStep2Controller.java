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

package bisq.desktop.main.content.user.user_profile.create.step2;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.security.pow.ProofOfWork;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;

@Slf4j
public class CreateNewProfileStep2Controller implements InitWithDataController<CreateNewProfileStep2Controller.InitData> {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class InitData {
        private final Optional<KeyPair> tempKeyPair;
        private final ProofOfWork proofOfWork;
        private final String nickName;
        private final String nym;

        public InitData(Optional<KeyPair> tempKeyPair,
                        ProofOfWork proofOfWork,
                        String nickName,
                        String nym) {
            this.tempKeyPair = tempKeyPair;
            this.proofOfWork = proofOfWork;
            this.nickName = nickName;
            this.nym = nym;
        }
    }

    protected final CreateNewProfileStep2Model model;
    @Getter
    protected final CreateNewProfileStep2View view;
    protected final UserIdentityService userIdentityService;
    private final ServiceProvider serviceProvider;

    public CreateNewProfileStep2Controller(ServiceProvider serviceProvider) {
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        this.serviceProvider = serviceProvider;

        model = createModel();
        view = createView();
    }

    protected CreateNewProfileStep2View createView() {
        return new CreateNewProfileStep2View(model, this);
    }

    protected CreateNewProfileStep2Model createModel() {
        return new CreateNewProfileStep2Model();
    }

    @Override
    public void initWithData(InitData data) {
        model.setTempKeyPair(data.getTempKeyPair());
        model.setProofOfWork(Optional.of(data.getProofOfWork()));
        model.getNickName().set(data.getNickName());
        model.getNym().set(data.getNym());
        if (data.getTempKeyPair().isPresent()) {
            model.getRoboHashImage().set(RoboHash.getImage(data.getProofOfWork().getPayload()));
        }
    }

    @Override
    public void onActivate() {
        model.getTerms().set("");
        model.getStatement().set("");
    }

    @Override
    public void onDeactivate() {
    }

    void onCancel() {
        OverlayController.hide();
    }

    void onQuit() {
        serviceProvider.getShutDownHandler().shutdown();
    }

    protected void onSave() {
        if (model.getProofOfWork().isEmpty()) {
            log.error("proofOfWork is not present");
            return;
        }
        model.getCreateProfileProgress().set(-1);
        model.getCreateProfileButtonDisabled().set(true);
        ProofOfWork proofOfWork = model.getProofOfWork().get();
        String nickName = model.getNickName().get();
        if (nickName.length() > UserProfile.MAX_LENGTH_NICK_NAME) {
            new Popup().warning(Res.get("onboarding.createProfile.nickName.tooLong", UserProfile.MAX_LENGTH_NICK_NAME)).show();
            return;
        }
        if (model.getTerms().get().length() > UserProfile.MAX_LENGTH_TERMS) {
            new Popup().warning(Res.get("user.userProfile.terms.tooLong", UserProfile.MAX_LENGTH_TERMS)).show();
            return;
        }
        if (model.getStatement().get().length() > UserProfile.MAX_LENGTH_STATEMENT) {
            new Popup().warning(Res.get("user.userProfile.statement.tooLong", UserProfile.MAX_LENGTH_STATEMENT)).show();
            return;
        }
        if (model.getTempKeyPair().isPresent()) {
            KeyPair keyPair = model.getTempKeyPair().get();
            userIdentityService.createAndPublishNewUserProfile(
                            nickName,
                            keyPair,
                            proofOfWork,
                            model.getTerms().get(),
                            model.getStatement().get())
                    .whenComplete((chatUserIdentity, throwable) -> UIThread.run(() -> {
                        if (throwable == null) {
                            model.getCreateProfileProgress().set(0);
                            close();
                        } else {
                            //todo
                        }
                    }));
        }
    }

    protected void close() {
        OverlayController.hide();
    }
}