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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state.states.*;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class TradeStateController implements Controller {
    @Getter
    private final TradeStateView view;
    private final TradeStateModel model;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final SettingsService settingsService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final DefaultApplicationService applicationService;
    private final TradePhaseBox tradePhaseBox;
    private Subscription isCollapsedPin;
    private Pin tradeRulesConfirmedPin, bisqEasyTradeStatePin;

    public TradeStateController(DefaultApplicationService applicationService, Consumer<UserProfile> openUserProfileSidebarHandler) {
        this.applicationService = applicationService;
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        settingsService = applicationService.getSettingsService();
        bisqEasyTradeService = applicationService.getTradeService().getBisqEasyTradeService();

        TradeWelcome tradeWelcome = new TradeWelcome();
        tradePhaseBox = new TradePhaseBox(applicationService);

        model = new TradeStateModel();
        view = new TradeStateView(model, this, tradeWelcome.getView().getRoot(), tradePhaseBox.getView().getRoot());
    }

    public void setSelectedChannel(BisqEasyPrivateTradeChatChannel channel) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        BisqEasyOffer bisqEasyOffer = channel.getBisqEasyOffer();
        boolean maker = isMaker(bisqEasyOffer);
        NetworkId takerNetworkId = maker ?
                channel.getPeer().getNetworkId() :
                myUserIdentity.getUserProfile().getNetworkId();
        String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
        Optional<BisqEasyTrade> optionalBisqEasyTrade = bisqEasyTradeService.findTrade(tradeId);
        if (optionalBisqEasyTrade.isEmpty()) {
            new Popup().warning(Res.get("bisqEasy.tradeState.warn.noTradeFound")).show();
            return;
        }
        BisqEasyTrade bisqEasyTrade = optionalBisqEasyTrade.orElseThrow();

        tradePhaseBox.setSelectedChannel(channel);
        tradePhaseBox.setBisqEasyTrade(bisqEasyTrade);
        model.setBisqEasyTrade(bisqEasyTrade);

        boolean isSeller = bisqEasyTrade.isSeller();
        if (bisqEasyTradeStatePin != null) {
            bisqEasyTradeStatePin.unbind();
        }
        bisqEasyTradeStatePin = bisqEasyTrade.tradeStateObservable().addObserver(state -> {
            UIThread.run(() -> {
                switch (state) {
                    case INIT:
                        break;
                    case TAKER_SENT_TAKE_OFFER_REQUEST:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE:
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE:
                        if (isSeller) {
                            model.getStateInfoVBox().set(new SellerState1(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        } else {
                            model.getStateInfoVBox().set(new BuyerState1(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        }
                        break;
                    case SELLER_SENT_ACCOUNT_DATA:
                        model.getStateInfoVBox().set(new SellerState2(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        break;
                    case BUYER_RECEIVED_ACCOUNT_DATA:
                        model.getStateInfoVBox().set(new BuyerState2(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        break;
                    case BUYER_SENT_FIAT_SENT_CONFIRMATION:
                        model.getStateInfoVBox().set(new BuyerState3(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        break;
                    case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
                        model.getStateInfoVBox().set(new SellerState3(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        break;
                    case SELLER_SENT_BTC_SENT_CONFIRMATION:
                        model.getStateInfoVBox().set(new SellerState4(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        break;
                    case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                        model.getStateInfoVBox().set(new BuyerState4(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        break;
                    case BTC_CONFIRMED:
                        if (isSeller) {
                            model.getStateInfoVBox().set(new SellerState5(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        } else {
                            model.getStateInfoVBox().set(new BuyerState5(applicationService, bisqEasyTrade, channel).getView().getRoot());
                        }
                        break;
                    default:
                        log.error(state.name());
                }
            });
        });

        String directionString = isSeller ?
                Res.get("offer.selling").toUpperCase() :
                Res.get("offer.buying").toUpperCase();
        AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
        String baseAmountString = OfferAmountFormatter.formatBaseSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
        String quoteAmountString = OfferAmountFormatter.formatQuoteSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
        FiatPaymentMethodSpec fiatPaymentMethodSpec = bisqEasyOffer.getQuoteSidePaymentMethodSpecs().get(0);
        String paymentMethodName = fiatPaymentMethodSpec.getPaymentMethod().getDisplayString();
        model.getHeadline().set(Res.get("bisqEasy.tradeState.header.headline",
                directionString,
                baseAmountString,
                quoteAmountString,
                paymentMethodName));
    }

    @Override
    public void onActivate() {
        model.getIsCollapsed().set(settingsService.getCookie().asBoolean(CookieKey.TRADE_ASSISTANT_COLLAPSED).orElse(false));
        tradeRulesConfirmedPin = settingsService.getTradeRulesConfirmed().addObserver(e -> applyVisibility());
        isCollapsedPin = EasyBind.subscribe(model.getIsCollapsed(), isCollapsed -> applyVisibility());
    }

    @Override
    public void onDeactivate() {
        tradeRulesConfirmedPin.unbind();
        isCollapsedPin.unsubscribe();
        if (bisqEasyTradeStatePin != null) {
            bisqEasyTradeStatePin.unbind();
        }
    }

    void onExpand() {
        setIsCollapsed(false);
    }

    void onCollapse() {
        setIsCollapsed(true);
    }

    void onHeaderClicked() {
        setIsCollapsed(!model.getIsCollapsed().get());
    }

    private void setIsCollapsed(boolean value) {
        model.getIsCollapsed().set(value);
        settingsService.setCookie(CookieKey.TRADE_ASSISTANT_COLLAPSED, value);
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }

    private void applyVisibility() {
        boolean tradeRulesConfirmed = settingsService.getTradeRulesConfirmed().get();
        boolean isExpanded = !model.getIsCollapsed().get();
        model.getTradeWelcomeVisible().set(isExpanded && !tradeRulesConfirmed);
        model.getPhaseAndInfoBoxVisible().set(isExpanded && tradeRulesConfirmed);
    }
}