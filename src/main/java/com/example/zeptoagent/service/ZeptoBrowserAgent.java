package com.example.zeptoagent.service;

import com.example.zeptoagent.model.Item;
import com.microsoft.playwright.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class ZeptoBrowserAgent {

    private static final String ZEPTO = "https://www.zepto.com/";

    private Consumer<String> logger = s -> {};

    public void setLogger(Consumer<String> logger) {
        this.logger = logger == null ? s -> {} : logger;
    }

    public void run(List<Item> items) {

        if (items == null || items.isEmpty()) {
            logger.accept("No items to order.");
            return;
        }

        try (Playwright playwright = Playwright.create()) {

            logger.accept("Starting Chromium...");

            boolean headless = Boolean.parseBoolean(
                System.getenv().getOrDefault("PLAYWRIGHT_HEADLESS", "false")
            );

            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(headless)
            );

            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            logger.accept("Opening Zepto...");

            page.navigate(ZEPTO);

            page.waitForTimeout(4000);

            logger.accept(
                "Zepto opened. Complete login/location selection if requested."
            );

            /*
             * Process each requested item.
             */
            for (Item item : items) {

                logger.accept(
                    "Processing: " +
                    item.quantity() +
                    " x " +
                    item.name()
                );

                boolean searched = searchProduct(page, item.name());

                if (!searched) {
                    logger.accept(
                        "Could not search for " + item.name()
                    );
                    continue;
                }

                boolean added = addFirstProduct(page, item.quantity());

                if (!added) {
                    logger.accept(
                        "Could not safely add " + item.name()
                    );
                    continue;
                }

                logger.accept(
                    "Added " +
                    item.quantity() +
                    " x " +
                    item.name()
                );

                page.waitForTimeout(1000);
            }

            /*
             * IMPORTANT:
             * Stop here. Do NOT checkout or make payment.
             */
            logger.accept("--------------------------------");
            logger.accept("All requested products processed.");
            logger.accept("Opening cart for your review...");

            openCart(page);

            logger.accept(
                "STOPPED BEFORE CHECKOUT/PAYMENT."
            );

            logger.accept(
                "Please review the cart and complete the purchase yourself."
            );

            logger.accept(headless ? "Cloud browser session finished." : "Browser will remain open.");

            /*
             * In cloud/headless mode the request must be allowed to finish.
             * Locally, keep the visible browser open for manual review.
             */
            if (!headless) {
                while (browser.isConnected() && !page.isClosed()) {
                    Thread.sleep(1000);
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            logger.accept("Agent interrupted.");

        } catch (Exception e) {

            logger.accept(
                "Browser agent error: " + e.getMessage()
            );
        }
    }

    private boolean searchProduct(Page page, String product) {

        logger.accept("Searching: " + product);

        try {

            Locator search = findSearchBox(page);

            if (search == null) {
                logger.accept("Search box was not found.");
                return false;
            }

            search.click();

            search.fill(product);

            search.press("Enter");

            page.waitForTimeout(2500);

            logger.accept(
                "Search results loaded for: " + product
            );

            return true;

        } catch (Exception e) {

            logger.accept(
                "Search failed: " + e.getMessage()
            );

            return false;
        }
    }

    private Locator findSearchBox(Page page) {

        String[] selectors = {
            "input[placeholder*='Search' i]",
            "input[aria-label*='Search' i]",
            "input[type='search']",
            "input[name*='search' i]",
            "[role='textbox']",
            "[contenteditable='true']"
        };

        for (String selector : selectors) {

            try {

                Locator locator =
                    page.locator(selector).first();

                if (locator.count() > 0 &&
                    locator.isVisible()) {

                    return locator;
                }

            } catch (Exception ignored) {
            }
        }

        /*
         * Zepto's visible search control.
         */
        try {

            Locator searchText =
                page.getByText(
                    "Search for",
                    new Page.GetByTextOptions()
                        .setExact(false)
                ).first();

            if (searchText.count() > 0 &&
                searchText.isVisible()) {

                searchText.click();

                page.waitForTimeout(700);

            }

        } catch (Exception ignored) {
        }

        for (String selector : selectors) {

            try {

                Locator locator =
                    page.locator(selector).first();

                if (locator.count() > 0 &&
                    locator.isVisible()) {

                    return locator;
                }

            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private boolean addFirstProduct(Page page, int quantity) {

        try {

            logger.accept("Looking for product ADD button...");

            Locator addButtons =
                page.getByText(
                    "ADD",
                    new Page.GetByTextOptions()
                        .setExact(true)
                );

            int count = addButtons.count();

            logger.accept(
                "Found " + count + " ADD button(s)."
            );

            if (count == 0) {
                logger.accept("No ADD button found.");
                return false;
            }

            /*
             * Select the first visible ADD button.
             *
             * This is intentionally conservative:
             * we don't click random page controls.
             */
            Locator add = null;

            for (int i = 0; i < count; i++) {

                Locator candidate = addButtons.nth(i);

                try {

                    if (candidate.isVisible()) {
                        add = candidate;
                        break;
                    }

                } catch (Exception ignored) {
                }
            }

            if (add == null) {
                logger.accept(
                    "No visible ADD button found."
                );
                return false;
            }

            add.scrollIntoViewIfNeeded();

            logger.accept(
                "Adding first matching product..."
            );

            add.click();

            page.waitForTimeout(1200);

            /*
             * Quantity 1 is already satisfied.
             */
            if (quantity <= 1) {
                return true;
            }

            /*
             * Try to increase quantity using visible
             * plus buttons near the selected product.
             */
            for (int i = 1; i < quantity; i++) {

                Locator plusButtons =
                    page.getByText(
                        "+",
                        new Page.GetByTextOptions()
                            .setExact(true)
                    );

                boolean clicked = false;

                for (int j = 0;
                     j < plusButtons.count();
                     j++) {

                    Locator plus =
                        plusButtons.nth(j);

                    try {

                        if (plus.isVisible()) {

                            plus.click();

                            page.waitForTimeout(500);

                            clicked = true;
                            break;
                        }

                    } catch (Exception ignored) {
                    }
                }

                if (!clicked) {

                    logger.accept(
                        "Could not find + button for quantity."
                    );

                    logger.accept(
                        "Product was added with the quantity reached."
                    );

                    break;
                }
            }

            return true;

        } catch (Exception e) {

            logger.accept(
                "Add-product operation failed: " +
                e.getMessage()
            );

            return false;
        }
    }

    private void openCart(Page page) {

        try {

            /*
             * Current Zepto page has a visible Cart control.
             */
            Locator cart =
                page.getByText(
                    "Cart",
                    new Page.GetByTextOptions()
                        .setExact(true)
                ).first();

            if (cart.count() > 0 &&
                cart.isVisible()) {

                cart.click();

                page.waitForTimeout(1500);

                logger.accept("Cart opened.");

                return;
            }

        } catch (Exception ignored) {
        }

        /*
         * Fallback: navigate to the cart URL.
         */
        try {

            page.navigate(ZEPTO + "?cart=open");

            page.waitForTimeout(1500);

            logger.accept(
                "Cart opened using the cart page."
            );

        } catch (Exception e) {

            logger.accept(
                "Could not open cart: " +
                e.getMessage()
            );
        }
    }
}