<?php
namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class CashSmsController extends Controller
{
    public function store(Request $request)
    {
        abort_unless(hash_equals((string) env('CASH_SMS_TOKEN'), (string) $request->bearerToken()), 401);
        if ($request->boolean('test')) return response()->json(['ok' => true]);

        $data = $request->validate([
            'provider' => 'required|in:vodafone_cash,orange_cash',
            'customer_phone' => ['required','regex:/^01[0125][0-9]{8}$/'],
            'amount' => 'required|numeric|min:1',
            'transaction_id' => 'required|string|max:64',
            'raw_text' => 'required|string|max:2000',
        ]);

        return DB::transaction(function () use ($data) {
            $duplicate = DB::table('cash_sms_events')->where('transaction_id', $data['transaction_id'])->exists();
            if ($duplicate) return response()->json(['ok' => true, 'status' => 'duplicate']);

            DB::table('cash_sms_events')->insert([
                ...$data, 'created_at' => now(), 'updated_at' => now(),
            ]);

            $payment = DB::table('payments')
                ->where('status', 'pending')
                ->where('customer_phone', $data['customer_phone'])
                ->where('amount', $data['amount'])
                ->lockForUpdate()->first();

            if (!$payment) return response()->json(['ok' => true, 'status' => 'unmatched']);

            DB::table('payments')->where('id', $payment->id)->update([
                'status' => 'paid', 'transaction_id' => $data['transaction_id'], 'paid_at' => now(), 'updated_at' => now(),
            ]);
            return response()->json(['ok' => true, 'status' => 'paid', 'payment_id' => $payment->id]);
        });
    }
}
