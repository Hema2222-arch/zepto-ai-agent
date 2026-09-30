package com.example.zeptoagent.service;

import com.example.zeptoagent.model.Item;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
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
                System.getenv().getOrDefault(
                    "PLAYWRIGHT_HEADLESS",
                    "false"
                )
            );

            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(headless)
            );

            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            logger.accept("Opening Zepto...");

            page.navigate(
                ZEPTO,
                new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60000)
            );

            page.waitForTimeout(8000);

            logger.accept("Zepto opened.");
            logger.accept("Current URL: " + page.url());

            try {
                logger.accept("Page title: " + page.title());
            } catch (Exception ignored) {
                logger.accept("Page title could not be read.");
            }

            logger.accept("Checking for search box...");

            waitForSearchInterface(page);

            for (Item item : items) {

                logger.accept(
                    "Processing: " +
                    item.quantity() +
                    " x " +
                    item.name()
                );

                boolean searched = searchProduct(
                    page,
                    item.name()
                );

                if (!searched) {
                    logger.accept(
                        "Could not search for " +
                        item.name()
                    );
                    continue;
                }

                boolean added = addFirstProduct(
                    page,
                    item.quantity()
                );

                if (!added) {
                    logger.accept(
                        "Could not safely add " +
                        item.name()
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

            logger.accept(
                headless
                    ? "Cloud browser session finished."
                    : "Browser will remain open."
            );

            if (!headless) {
                while (
                    browser.isConnected() &&
                    !page.isClosed()
                ) {
                    Thread.sleep(1000);
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            logger.accept("Agent interrupted.");

        } catch (Exception e) {

            logger.accept(
                "Browser agent error: " +
                e.getClass().getSimpleName() +
                ": " +
                e.getMessage()
            );
        }
    }

    private void waitForSearchInterface(Page page) {

        logger.accept(
            "Waiting for Zepto search interface..."
        );

        for (int attempt = 1; attempt <= 12; attempt++) {

            Locator search = findSearchBox(page);

            if (search != null) {
                logger.accept("Search interface detected.");
                return;
            }

            logger.accept(
                "Search interface not ready yet. Attempt " +
                attempt +
                "/12"
            );

            page.waitForTimeout(1000);
        }

        logger.accept(
            "Search interface was not detected after waiting."
        );

        logger.accept(
            "Final URL: " + page.url()
        );

        try {
            logger.accept(
                "Final page title: " + page.title()
            );
        } catch (Exception ignored) {
        }
    }

    private boolean searchProduct(
        Page page,
        String product
    ) {

        logger.accept("Searching: " + product);

        try {

            Locator search = findSearchBox(page);

            if (search == null) {

                logger.accept("Search box was not found.");
                logger.accept("Current URL: " + page.url());

                return false;
            }

            logger.accept(
                "Search box found. Entering: " + product
            );

            search.scrollIntoViewIfNeeded();
            search.click();
            search.fill(product);
            search.press("Enter");

            page.waitForTimeout(3000);

            logger.accept(
                "Search results loaded for: " + product
            );

            return true;

        } catch (Exception e) {

            logger.accept(
                "Search failed: " +
                e.getClass().getSimpleName() +
                ": " +
                e.getMessage()
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

                Locator locator = page.locator(selector);

                int count = locator.count();

                for (int i = 0; i < count; i++) {

                    Locator candidate = locator.nth(i);

                    if (
                        candidate.isVisible() &&
                        candidate.isEnabled()
                    ) {
                        return candidate;
                    }
                }

            } catch (Exception ignored) {
            }
        }

        String[] searchTexts = {
            "Search for",
            "Search"
        };

        for (String text : searchTexts) {

            try {

                Locator searchText =
                    page.getByText(
                        text,
                        new Page.GetByTextOptions()
                            .setExact(false)
                    );

                int count = searchText.count();

                for (int i = 0; i < count; i++) {

                    Locator candidate = searchText.nth(i);

                    if (candidate.isVisible()) {

                        candidate.scrollIntoViewIfNeeded();
                        candidate.click();

                        page.waitForTimeout(700);

                        for (String selector : selectors) {

                            try {

                                Locator input =
                                    page.locator(selector);

                                int inputCount = input.count();

                                for (
                                    int j = 0;
                                    j < inputCount;
                                    j++
                                ) {

                                    Locator actualInput =
                                        input.nth(j);

                                    if (
                                        actualInput.isVisible() &&
                                        actualInput.isEnabled()
                                    ) {
                                        return actualInput;
                                    }
                                }

                            } catch (Exception ignored) {
                            }
                        }
                    }
                }

            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private boolean addFirstProduct(
        Page page,
        int quantity
    ) {

        try {

            logger.accept(
                "Looking for product ADD button..."
            );

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

            Locator add = null;

            for (int i = 0; i < count; i++) {

                Locator candidate = addButtons.nth(i);

                try {

                    if (
                        candidate.isVisible() &&
                        candidate.isEnabled()
                    ) {
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

            if (quantity <= 1) {
                return true;
            }

            for (int i = 1; i < quantity; i++) {

                Locator plusButtons =
                    page.getByText(
                        "+",
                        new Page.GetByTextOptions()
                            .setExact(true)
                    );

                boolean clicked = false;

                for (
                    int j = 0;
                    j < plusButtons.count();
                    j++
                ) {

                    Locator plus = plusButtons.nth(j);

                    try {

                        if (
                            plus.isVisible() &&
                            plus.isEnabled()
                        ) {

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
                e.getClass().getSimpleName() +
                ": " +
                e.getMessage()
            );

            return false;
        }
    }

    private void openCart(Page page) {

        try {

            Locator cart =
                page.getByText(
                    "Cart",
                    new Page.GetByTextOptions()
                        .setExact(true)
                ).first();

            if (
                cart.count() > 0 &&
                cart.isVisible()
            ) {

                cart.click();

                page.waitForTimeout(1500);

                logger.accept("Cart opened.");

                return;
            }

        } catch (Exception ignored) {
        }

        try {

            page.navigate(
                ZEPTO + "?cart=open",
                new Page.NavigateOptions()
                    .setWaitUntil(
                        WaitUntilState.DOMCONTENTLOADED
                    )
                    .setTimeout(30000)
            );

            page.waitForTimeout(1500);

            logger.accept(
                "Cart opened using the cart page."
            );

        } catch (Exception e) {

            logger.accept(
                "Could not open cart: " +
                e.getClass().getSimpleName() +
                ": " +
                e.getMessage()
            );
        }
    }
}