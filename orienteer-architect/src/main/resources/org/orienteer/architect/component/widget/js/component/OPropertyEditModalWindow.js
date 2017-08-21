/**
 * Modal window for edit or create {@link OArchitectOProperty}
 * @param property {@link OArchitectOProperty} for create or edit
 * @param containerId id of element which contains modal window
 * @param onDestroy callback function which calls when modal window destroy
 * @param create true if create new property
 * @constructor
 */
var OPropertyEditModalWindow = function (property, containerId, onDestroy, create) {
    OClassEditModalWindow.apply(this, arguments);
    this.create = create;
};

OPropertyEditModalWindow.prototype = Object.create(OClassEditModalWindow.prototype);
OPropertyEditModalWindow.prototype.constructor = OPropertyEditModalWindow;

OPropertyEditModalWindow.prototype.orientDbTypes = OArchitectOType.types;

OPropertyEditModalWindow.prototype.createContent = function (panel, head, body) {
    var select = this.createOTypeSelect(this.create);
    var input = this.createNameInput(this.create);
    this.addValueBlock(body, input, select);
    this.addButtonBlock(body, input, select);
    this.addHeadBlock(head, this.create);
};

OPropertyEditModalWindow.prototype.addValueBlock = function (body, input, select) {
    var valueBlock = this.createValueBlock();
    valueBlock.appendChild(this.createLabel(localizer.name + ':'));
    valueBlock.appendChild(input);
    valueBlock.appendChild(this.createLabel(localizer.type + ':'));
    valueBlock.appendChild(select);
    body.appendChild(valueBlock);
};

OPropertyEditModalWindow.prototype.addHeadBlock = function (head, create) {
    this.createHeadBlock(head, create ? localizer.createProperty : localizer.editProperty,
        OArchitectConstants.FA_ALIGN_JUSTIFY);
};

OPropertyEditModalWindow.prototype.addButtonBlock = function (body, input, select) {
    var buttonBlock = this.createButtonBlock();
    var okBut = this.createOkButton(localizer.ok, input, select);
    var cancelBut = this.createCancelButton(localizer.cancel);
    buttonBlock.appendChild(okBut);
    buttonBlock.appendChild(cancelBut);
    body.appendChild(buttonBlock);
};

OPropertyEditModalWindow.prototype.createOTypeSelect = function (createNewOProperty) {
    var select = document.createElement('select');
    select.classList.add('form-control');
    var types = this.orientDbTypes;
    for (var i = 0; i < types.length; i++) {
        var option = document.createElement('option');
        option.setAttribute('value', types[i]);
        option.innerHTML = types[i];
        select.appendChild(option);
    }
    if (!createNewOProperty && this.value.type != null) {
        var index = types.indexOf(this.value.type.toUpperCase());
        if (index > -1)
            select.selectedIndex = index;
    }
    return select;
};

//TODO: validate user input
OPropertyEditModalWindow.prototype.createNameInput = function (createNewOProperty) {
    this.input = document.createElement('input');
    this.input.classList.add('form-control');
    this.input.setAttribute('type', 'text');
    if (!createNewOProperty) {
        this.input.value = this.value.name;
    }
    return this.input;
};


OPropertyEditModalWindow.prototype.createOkButton = function (label, nameField, typeSelect) {
    var button = this.newButton(label, OArchitectConstants.BUTTON_PRIMARY_CLASS);
    button.addEventListener('click', this.createOkButtonOnClickBehavior(nameField, typeSelect));
    button.style.float = 'right';
    button.style.marginRight = '10px';
    button.style.marginBottom = '10px';
    return button;
};

OPropertyEditModalWindow.prototype.createOkButtonOnClickBehavior = function (nameField, typeSelect) {
    var modal = this;
    return function () {
        if (nameField.value.length > 0) {
            var newName = nameField.value;
            modal.value.setName(newName, function (property, msg) {
                if (property.name === newName) {
                    modal.value.setType(typeSelect.options[typeSelect.selectedIndex].value);
                    modal.destroy(modal.OK);
                } else {
                    modal.showErrorFeedback(msg);
                }
            });
        }
    };
};