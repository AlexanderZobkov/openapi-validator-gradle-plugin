package com.github.openapivalidatorgradleplugin

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import groovy.transform.CompileDynamic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

/**
 * Validates specified files as request or response bodies for specified call in OpenAPI specification.
 */
@CompileDynamic
class ValidateRequestResponseTask extends DefaultTask {

    /**
     * OpenAPI specification file.
     */
    @InputFile
    File specificationFile

    /**
     * Location of request files to validate.
     */
    @InputFiles
    FileCollection requestFiles = project.files()

    /**
     * Location of response files to validate.
     */
    @InputFiles
    FileCollection responseFiles = project.files()

    /**
     * Request path.
     */
    @Input
    String path

    /**
     * Request method.
     */
    @Input
    String method

    /**
     * Response status code.
     */
    @Input
    int status = 200

    /**
     * Request content type.
     */
    @Input
    String requestContentType = 'application/merge-patch+json'

    /**
     * Response content type.
     */
    @Input
    String responseContentType = 'application/json'

    /**
     * Creates an instance.
     */
    ValidateRequestResponseTask() {
        group = 'verification'
        description = 'Validates files as responses bodies against OpenAPI specification'
    }

    /**
     * Validates all specified files.
     */
    @TaskAction
    void validate() {
        OpenApiInteractionValidator validator =
                OpenApiInteractionValidator.createFor('file:' + specificationFile.absolutePath).
                        build()
        requestFiles?.each { requestFile ->
            validator.validateRequest(
                    (SimpleRequest.Builder."${method.toLowerCase()}"(path) as SimpleRequest.Builder).
                            withAccept(responseContentType).
                            withContentType(requestContentType).
                            withBody(requestFile.text).
                            build()).with { result ->
                if (result.hasErrors()) {
                    throw new GradleException(
                            "$requestFile does not match request $method $path from $specificationFile:" +
                                    " ${result.messages.first()}")
                }
            }
        }
        responseFiles?.each { responseFile ->
            validator.validateResponse(
                    path,
                    Request.Method.valueOf(method.toUpperCase()),
                    SimpleResponse.Builder.status(status).
                            withContentType(responseContentType).
                            withBody(responseFile.text).
                            build()).with { result ->
                if (result.hasErrors()) {
                    throw new GradleException(
                            "$responseFile does not match response $method $path $status from $specificationFile:" +
                                    " ${result.messages.first()}")
                }
            }
        }
    }
}
