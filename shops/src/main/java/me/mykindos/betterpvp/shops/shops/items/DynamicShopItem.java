package me.mykindos.betterpvp.shops.shops.items;

import me.mykindos.betterpvp.shops.shops.items.data.PolynomialData;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.bukkit.Material;


public class DynamicShopItem extends ShopItem {

    private final PolynomialData polynomialData;
    private final PolynomialFunctionLagrangeForm buyPolynomial;
    private final PolynomialFunctionLagrangeForm sellPolynomial;

    public DynamicShopItem(String store, String itemName, Material material, int slot, int page, byte data, PolynomialData polynomialData) {
        super(store, itemName, material, slot, page, data);
        this.polynomialData = polynomialData;

        var buyX = new double[]{0, polynomialData.getBaseStock(), polynomialData.getMaxStock()};
        var buyY = new double[]{polynomialData.getMaxBuyPrice(), polynomialData.getBaseBuyPrice(), polynomialData.getMinBuyPrice()};

        var sellX = new double[]{polynomialData.getMaxStock(), polynomialData.getBaseStock(), 0};
        var sellY = new double[]{polynomialData.getMinSellPrice(), polynomialData.getBaseSellPrice(), polynomialData.getMaxSellPrice()};

        this.buyPolynomial = new PolynomialFunctionLagrangeForm(buyX, buyY);
        this.sellPolynomial = new PolynomialFunctionLagrangeForm(sellX, sellY);
    }

    @Override
    public int getBuyPrice() {
        return (int) buyPolynomial.value(polynomialData.getCurrentStock());
    }

    @Override
    public int getSellPrice() {
        return (int) sellPolynomial.value(polynomialData.getCurrentStock());
    }

    public int getCurrentStock() {
        return polynomialData.getCurrentStock();
    }

    public void setCurrentStock(int amount) {
        polynomialData.setCurrentStock(amount);
    }
}
