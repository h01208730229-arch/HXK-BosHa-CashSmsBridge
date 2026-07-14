<?php
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\CashSmsController;
Route::post('/cash/sms', [CashSmsController::class, 'store']);
