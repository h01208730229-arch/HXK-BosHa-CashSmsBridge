<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
return new class extends Migration {
 public function up(): void { Schema::create('cash_sms_events', function (Blueprint $t) {
  $t->id(); $t->string('provider'); $t->string('customer_phone',11); $t->decimal('amount',12,2);
  $t->string('transaction_id',64)->unique(); $t->text('raw_text'); $t->timestamps();
 }); }
 public function down(): void { Schema::dropIfExists('cash_sms_events'); }
};
