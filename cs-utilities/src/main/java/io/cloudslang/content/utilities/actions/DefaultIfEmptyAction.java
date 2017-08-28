/*******************************************************************************
 * (c) Copyright 2017 Hewlett-Packard Enterprise Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 *
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/
package io.cloudslang.content.utilities.actions;

import com.hp.oo.sdk.content.annotations.Action;
import com.hp.oo.sdk.content.annotations.Output;
import com.hp.oo.sdk.content.annotations.Param;
import com.hp.oo.sdk.content.annotations.Response;
import com.hp.oo.sdk.content.plugin.ActionMetadata.MatchType;
import com.hp.oo.sdk.content.plugin.ActionMetadata.ResponseType;
import io.cloudslang.content.constants.BooleanValues;
import io.cloudslang.content.constants.ReturnCodes;
import io.cloudslang.content.utilities.services.DefaultIfEmptyService;

import java.util.Map;

import static io.cloudslang.content.constants.OutputNames.*;
import static io.cloudslang.content.constants.ReturnCodes.FAILURE;
import static io.cloudslang.content.constants.ReturnCodes.SUCCESS;
import static io.cloudslang.content.utilities.entities.constants.Inputs.*;
import static io.cloudslang.content.utils.BooleanUtilities.toBoolean;
import static io.cloudslang.content.utils.OutputUtilities.getFailureResultsMap;
import static io.cloudslang.content.utils.OutputUtilities.getSuccessResultsMap;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Created by moldovai on 8/21/2017.
 */
public class DefaultIfEmptyAction {

    /**
     * This operation checks if a string is blank or empty and if it's true a default value
     * will be assigned instead of the initial string.
     *
     * @param initialValue The initial string.
     * @param defaultValue The default value used to replace the initial string.
     * @param trim         A variable used to check if the initial string is blank or empty.
     * @return a map containing the output of the operation. Keys present in the map are:
     * returnResult - This will contain the replaced string with the default value.
     * exception - In case of success response, this result is empty. In case of failure response,
     * this result contains the java stack trace of the runtime exception.
     * returnCode - The returnCode of the operation: 0 for success, -1 for failure.
     */

    @Action(name = "Default value if the initial string is blank or empty",
            outputs = {
                    @Output(RETURN_CODE),
                    @Output(RETURN_RESULT),
                    @Output(EXCEPTION),
            },
            responses = {
                    @Response(text = SUCCESS, field = RETURN_CODE, value = SUCCESS, matchType = MatchType.COMPARE_EQUAL, responseType = ResponseType.RESOLVED),
                    @Response(text = FAILURE, field = RETURN_CODE, value = ReturnCodes.FAILURE, matchType = MatchType.COMPARE_EQUAL, responseType = ResponseType.ERROR, isOnFail = true)
            })
    public Map<String, String> execute(
            @Param(value = INITIAL_VALUE) String initialValue,
            @Param(value = DEFAULT_VALUE, required = true) String defaultValue,
            @Param(value = TRIM) String trim) {

        try {
            trim = defaultIfBlank(trim, BooleanValues.TRUE);

            final boolean validTrim = toBoolean(trim);

            return getSuccessResultsMap(DefaultIfEmptyService.defaultIfBlankOrEmpty(initialValue, defaultValue, validTrim));

        } catch (Exception e) {
            return getFailureResultsMap(e);
        }
    }
}