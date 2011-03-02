////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3;

import macromedia.asc.util.ObjectList;

/**
 * @author Clement Wong
 */
public interface Configuration
{
	/**
	 * Generate SWFs for debugging
	 */
	boolean debug();

	int dialect();
	boolean adjustOpDebugLine();

	/**
	 * Run the AS3 compiler in strict mode
	 */
	boolean strict();

	/**
	 * Enable asc -warnings
	 */
	boolean warnings();

	/**
	 * Generate asdoc
	 */
	boolean doc();

	/**
	 * user-defined AS3 file encoding
	 */
	String getEncoding();
    
    /**
     * Configuration settings (ConfigVars) from <code>--compiler.define</code>
     * are retrieved using this getter.
     *  
     * @return ObjectList<macromedia.asc.embedding.ConfigVar>
     */
    ObjectList getDefine();

	/**
	 * Whether to export metadata into ABCs
	 */
	public boolean metadataExport();

	// coach warnings

	public boolean warn_array_tostring_changes();

	public boolean warn_assignment_within_conditional();

	public boolean warn_bad_array_cast();

	public boolean warn_bad_bool_assignment();

	public boolean warn_bad_date_cast();

	public boolean warn_bad_es3_type_method();

	public boolean warn_bad_es3_type_prop();

	public boolean warn_bad_nan_comparison();

	public boolean warn_bad_null_assignment();

	public boolean warn_bad_null_comparison();

	public boolean warn_bad_undefined_comparison();

	public boolean warn_boolean_constructor_with_no_args();

	public boolean warn_changes_in_resolve();

	public boolean warn_class_is_sealed();

	public boolean warn_const_not_initialized();

	public boolean warn_constructor_returns_value();

	public boolean warn_deprecated_event_handler_error();

	public boolean warn_deprecated_function_error();

	public boolean warn_deprecated_property_error();

	public boolean warn_duplicate_argument_names();

	public boolean warn_duplicate_variable_def();

	public boolean warn_for_var_in_changes();

	public boolean warn_import_hides_class();

	public boolean warn_instance_of_changes();

	public boolean warn_internal_error();

	public boolean warn_level_not_supported();

	public boolean warn_missing_namespace_decl();

	public boolean warn_negative_uint_literal();

	public boolean warn_no_constructor();

	public boolean warn_no_explicit_super_call_in_constructor();

	public boolean warn_no_type_decl();

	public boolean warn_number_from_string_changes();

	public boolean warn_scoping_change_in_this();

	public boolean warn_slow_text_field_addition();

	public boolean warn_unlikely_function_value();

	public boolean warn_xml_class_has_changed();
}
