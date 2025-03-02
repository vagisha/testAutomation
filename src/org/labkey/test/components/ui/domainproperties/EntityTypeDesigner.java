package org.labkey.test.components.ui.domainproperties;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.domain.DomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;
import static org.labkey.test.WebDriverWrapper.waitFor;

public abstract class EntityTypeDesigner<T extends EntityTypeDesigner<T>> extends DomainDesigner<EntityTypeDesigner<T>.ElementCache>
{
    public EntityTypeDesigner(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected abstract T getThis();

    public T removeField(boolean confirmDialogExpected, String fieldName)
    {
        getFieldsPanel().removeField(fieldName, confirmDialogExpected);
        sleep(250); // wait for collapse animation
        return getThis();
    }

    public T addField(FieldDefinition field)
    {
        return addFields(List.of(field));
    }

    public T addFields(List<FieldDefinition> fields)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        boolean firstField = true;

        for (FieldDefinition field : fields)
        {
            if (firstField && fieldsPanel.isManuallyDefineFieldsPresent())
            {
                fieldsPanel.manuallyDefineFields(field.getName());
                fieldsPanel.setField(field);
            }
            else
            {
                fieldsPanel.addField(field);
            }

            firstField = false;
        }
        return getThis();
    }

    public String getName()
    {
        expandPropertiesPanel();
        return elementCache().nameInput.get();
    }

    public T setName(String name)
    {
        expandPropertiesPanel();
        elementCache().nameInput.set(name);
        return getThis();
    }

    public boolean isNameFieldEnabled()
    {
        String disabledValue = Locator.tagWithId("input", "entity-name").findWhenNeeded(getDriver()).getAttribute("disabled");
        return (null == disabledValue) || (!disabledValue.equalsIgnoreCase("true"));
    }

    public String getNameExpression()
    {
        expandPropertiesPanel();
        return elementCache().nameExpressionInput.get();
    }

    public T setNameExpression(String nameExpression)
    {
        expandPropertiesPanel();
        elementCache().nameExpressionInput.set(nameExpression);
        return getThis();
    }

    public String getNameExpressionPreview()
    {
        getWrapper().mouseOver(elementCache().helpTarget("Naming "));
        waitFor(()->elementCache().toolTip.isDisplayed(), "No tooltip was shown for the Name Expression.", 1_000);
        return elementCache().toolTip.getText();
    }

    public T dismissToolTip()
    {

        // As far as I can tell every entity designer page has a "Learn more" link. This link is inside the container
        // but far enough away from the other elements to make the tool tip for a given field go away.
        // Of course this may not be true in the future.
        getWrapper().mouseOver(elementCache().learnMoreLink);

        waitFor(()->!elementCache().toolTip.isDisplayed(), "The tool tip did not go away.", 1_000);

        return getThis();
    }

    /**
     * Check to see if the genId banner is visible.
     *
     * @return True if visible, false if not.
     */
    public boolean isGenIdVisible()
    {
        return elementCache().genIdAlert.isDisplayed();
    }

    /**
     * The genId value displayed in the genId banner.
     *
     * @return The current genId value.
     */
    public String getCurrentGenId()
    {
        // Protect against scenarios where the genId banner takes a moment to show up.
        waitFor(()->elementCache().genIdAlert.isDisplayed(),
                "The GenId banner is not visible.", 1_000);

        String rawText = elementCache().genIdAlert.getText().split("\n")[0];
        rawText = rawText.substring(rawText.indexOf(":")+1);
        return rawText.trim();
    }

    /**
     * Click the 'Edit GenId' button.
     *
     * @return A {@link GenIdDialog}
     */
    public GenIdDialog clickEditGenId()
    {
        // There is code that checks to see if the Edit genId button should be shown. Try to protect against that.
        waitFor(()->elementCache().editGenIdButton.isDisplayed(),
                "The 'Edit GenId' is not visible.", 1_000);

        elementCache().editGenIdButton.click();
        return new GenIdDialog();
    }

    /**
     * Check if the 'Reset GenId' button is visible.
     *
     * @return True if visible, false otherwise.
     */
    public boolean isResetGenIdVisible()
    {
        return elementCache().resetGenIdButton.isDisplayed();
    }

    /**
     * Click the 'Reset GenId' button and return the confirm dialog.
     *
     * @return A {@link ModalDialog} asking to confirm the reset.
     */
    public ModalDialog clickResetGenId()
    {
        // There is code that checks to see if the Reset genId button should be shown. Try to protect against that.
        waitFor(()->elementCache().resetGenIdButton.isDisplayed(),
                "The 'Reset GenId' is not visible.", 1_000);

        elementCache().resetGenIdButton.click();
        return new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Are you sure you want to reset genId").waitFor();
    }

    public String getAutoLinkDataToStudy()
    {
        expandPropertiesPanel();
        return elementCache().autoLinkDataToStudy.toString();
    }

    public T setAutoLinkDataToStudy(String value)
    {
        expandPropertiesPanel();
        elementCache().autoLinkDataToStudy.selectByVisibleText(value);
        return getThis();
    }

    public String getLinkedDatasetCategory()
    {
        expandPropertiesPanel();
        return elementCache().linkedDatasetCategory.get();
    }

    public T setLinkedDatasetCategory(String value)
    {
        expandPropertiesPanel();
        elementCache().linkedDatasetCategory.set(value);
        return getThis();
    }

    public String getDescription()
    {
        expandPropertiesPanel();
        return elementCache().descriptionInput.get();
    }

    public T setDescription(String description)
    {
        expandPropertiesPanel();
        elementCache().descriptionInput.set(description);
        return getThis();
    }

    public Optional<WebElement> optionalWarningAlert()
    {
        return elementCache().optionalWarningAlert();
    }


    /**
     * Dialog that allows the user to set the genId value.
     */
    public class GenIdDialog extends ModalDialog
    {

        protected final WebElement input = Locator.input("newgenidval").findWhenNeeded(this);

        public GenIdDialog()
        {
            super(new ModalDialogFinder(EntityTypeDesigner.this.getDriver()).withTitle("Are you sure you want to update genId"));
        }

        public String getGenId()
        {
            return getWrapper().getFormElement(input);
        }

        public GenIdDialog setGenId(String value)
        {
            getWrapper().setFormElement(input, value);
            return this;
        }

        public String clickUpdateExpectError()
        {
            Locators.dismissButton("Update").findElement(getComponentElement()).click();
            WebElement alert = Locator.tagWithClass("div", "alert-danger").findWhenNeeded(getComponentElement());

            WebDriverWrapper.waitFor(()->alert.isDisplayed(), "No error message was shown on the dialog.", 500);

            return alert.getText();
        }

    }

    protected class ElementCache extends DomainDesigner<?>.ElementCache
    {
        protected final Input nameInput = Input.Input(Locator.id("entity-name"), getDriver()).findWhenNeeded(this);
        protected final Input nameExpressionInput = Input.Input(Locator.id("entity-nameExpression"), getDriver()).waitFor(this);
        protected final Input descriptionInput = Input.Input(Locator.id("entity-description"), getDriver()).findWhenNeeded(this);

        protected final Select autoLinkDataToStudy = SelectWrapper.Select(Locator.id("entity-autoLinkTargetContainerId"))
                .findWhenNeeded(this);
        protected final Input linkedDatasetCategory = Input.Input(Locator.id("entity-autoLinkCategory"), getDriver()).findWhenNeeded(this);

        Optional<WebElement> optionalWarningAlert()
        {
            return BootstrapLocators.warningBanner.findOptionalElement(this);
        }

        public final WebElement learnMoreLink = Locator.linkContainingText("Learn more").findWhenNeeded(this);

        public final WebElement helpTarget(String divLabelText)
        {
            return Locator.xpath(String.format("//div[text()='%s']//span[@class='label-help-target']", divLabelText)).findWhenNeeded(this);
        }

        // Tool tips exist on the page, outside the scope of the domainDesigner, so scope the search accordingly.
        public final WebElement toolTip = Locator.tagWithId("div", "tooltip").refindWhenNeeded(getDriver());

        protected final WebElement genIdAlert = Locator.tagWithClass("div", "genid-alert").refindWhenNeeded(this);

        protected final WebElement editGenIdButton = Locator.tagWithClass("button", "edit-genid-btn").findWhenNeeded(this);

        protected final WebElement resetGenIdButton = Locator.tagWithClass("button", "reset-genid-btn").findWhenNeeded(this);

        protected final ReactSelect storedAmountDisplayUnitsSelect = ReactSelect.finder(getDriver())
                .withContainerClass("sampleset-metric-unit-select-container").timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);

        final Locator uniqueIdMsgLoc = Locator.tagWithClass("div", "uniqueid-msg");

    }

}
