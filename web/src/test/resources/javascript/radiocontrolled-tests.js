
module("$.radioControlled()");

function $is(selector, filter) {
	ok(jQuery(selector).is(filter), selector + " should be " + filter);
}

test("exists", function( assert ) {
	assert.ok(jQuery('body').radioControlled, "Plugin should be defined");
});

// The original behaviour for tabulaRadioActive plugin
test("radios setting disabled by ID", function(assert) {

	var $area = jQuery('#radioActiveArea');
	$area.find('[name=a]').radioControlled(); // all defaults or data-driven

	// should set up states initially
	$is('#RAgoodArea input', ':enabled');
	$is('#RAuglyArea input', ':disabled');

	$area.find('[value=bad]').click();
	$is('#RAgoodArea input', ':disabled');
	$is('#RAuglyArea input', ':disabled');

	$area.find('[value=ugly]').click();
	$is('#RAgoodArea input', ':disabled');
	$is('#RAuglyArea input', ':enabled');
});

// The original behaviour for radioDisble plugin
test("radios setting readonly by ", function(assert) {
	var $area = jQuery('#radioReadOnly');
	var $radios = $area.find('label input');

	// custom assertion
	function shouldBeReadonly(readonly, $container) {
		assert.equal($container.find('input').prop('readonly'), readonly, "Inputs in "+$container.selector+" should be readonly="+readonly);
		assert.equal($container.find('select').prop('readonly'), undefined, "Selects in "+$container.selector+" do not define readonly");
	}

	$radios.radioControlled({
		parentSelector: '.control-group',
		selector: '.controls',
		mode: 'readonly'
	});

	shouldBeReadonly(false, $area.find('.first .controls'));
	shouldBeReadonly(true, $area.find('.second .controls'));

	$area.find('.second label input').click();
	shouldBeReadonly(true, $area.find('.first .controls'));
	shouldBeReadonly(false, $area.find('.second .controls'));
});

test("select box hides div", function(assert) {
	var $area = jQuery('#selectShowArea');
	var $select = $area.find('select');

	$select.radioControlled({
		parentSelector: '.controls',
		mode: 'hidden'
	});

	$is('#selectShowArea .custom-text-area', ':hidden');

	$select.val('Year 2');
	$select.change(); // JS doesn't trigger change so do it ourselves.
	$is('#selectShowArea .custom-text-area', ':hidden');

	$select.val('');
	$select.change();
	$is('#selectShowArea .custom-text-area', ':visible');
})

test ("events", function(assert) {
	var $area = jQuery('#radioActiveArea');
	var $radios = $area.find('input[name=a]');

	var onEnableCalled = 0;
	var onDisableCalled = 0;

	$area.find('[name=egg-type]')
		.on('enable.radiocontrolled', function() {
			onEnableCalled++;
		})
		.on('disable.radiocontrolled', function() {
			onDisableCalled++;
		});

	$radios.radioControlled();

	assert.equal(onEnableCalled, 0, "No enable events initially fired");
	assert.equal(onDisableCalled, 0, "No disable events initially fired");

	$area.find('[value=bad]').click();
	assert.equal(onEnableCalled, 0, "No enable event fired");
	assert.equal(onDisableCalled, 1, "One disable event fired");

	$area.find('[value=ugly]').click();
	assert.equal(onEnableCalled, 0, "No enable event fired");
	assert.equal(onDisableCalled, 2, "One more disable event fired");

	$area.find('[value=good]').click();
	assert.equal(onEnableCalled, 1, "One enable event fired");
	assert.equal(onDisableCalled, 2, "No disable event fired");
})