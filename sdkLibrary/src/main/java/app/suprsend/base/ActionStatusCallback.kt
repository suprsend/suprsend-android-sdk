package app.suprsend.base

import app.suprsend.model.ApiResponse

interface ActionStatusCallback {
    fun onComplete(actionStatus: ApiResponse)
}