package com.umstj.fastnfc.ui.dashboard;

import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.umstj.fastnfc.R;
import com.umstj.fastnfc.databinding.FragmentDashboardBinding;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Objects;

public class DashboardFragment extends Fragment implements NfcAdapter.ReaderCallback {

    private FragmentDashboardBinding binding;
    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private EditText editText;
    private NfcAdapter nfcAdapter;
    private String ed;
    private Button button;
    private static final String TAG = "WriteNfcTagFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        editText = binding.editText;
        keyboardView = binding.keyboardView;
        keyboard = new Keyboard(getContext(), R.xml.custom_keybd);
        button = binding.submitButton;

        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(new KeyboardView.OnKeyboardActionListener() {
            @Override
            public void onPress(int primaryCode) {}

            @Override
            public void onRelease(int primaryCode) {}

            @Override
            public void onKey(int primaryCode, int[] keyCodes) {
                Editable editable = editText.getText();
                int start = editText.getSelectionStart();
                if (primaryCode == Keyboard.KEYCODE_DELETE) {
                    if (editable != null && start > 0) {
                        editable.delete(start - 1, start);
                    }
                } else {
                    editable.insert(start, Character.toString((char) primaryCode));

                }
                ed = editText.getText().toString();
                if(ed.length() >0){
                    if (ed.equals("000"))
                    {
                        ed = "0";
                    }
                    int number = Integer.parseInt(ed);
                    if (number > 255){
                        ed = "254";
                    }

                }else {
                    ed = "0";
                }
                button.setText(getText(R.string.btn_write)+":" + ed);



            }

            @Override
            public void onText(CharSequence text) {}

            @Override
            public void swipeLeft() {}

            @Override
            public void swipeRight() {}

            @Override
            public void swipeDown() {}

            @Override
            public void swipeUp() {}
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle button click here
                ed = editText.getText().toString();
                button.setText(getText(R.string.btn_write)+":" + ed);
            }
        });

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    showCustomKeyboard(view);
                } else {
                    hideCustomKeyboard();
                }
            }
        });

        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomKeyboard(v);
            }
        });

        return root;
    }

    private void showCustomKeyboard(View view) {
        keyboardView.setVisibility(View.VISIBLE);
        keyboardView.setEnabled(true);
        if (view != null) {
            ((InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void hideCustomKeyboard() {
        keyboardView.setVisibility(View.GONE);
        keyboardView.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        hideCustomKeyboard();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(getActivity());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableReaderMode(getActivity(), this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V, null);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ed = editText.getText().toString();
        button.setText(getText(R.string.btn_write)+":" + ed);
        nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter == null) {
            Toast.makeText(getActivity(), "NFC is not available on this device.", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        Log.d(TAG, "NFC Tag Discovered");
        Integer st1;
        byte b = 0x00;
        Log.e("Err",ed);
        try {

            st1 = Integer.parseInt(ed);
            b = (byte) (st1 & 0xff);

        }catch (Exception e){
            Log.e("Err", Objects.requireNonNull(e.getMessage()));

        }
        byte[] data = new byte[]{(byte) b , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        if (tag != null) {
            // 选择合适的方法进行写入
            if (MifareClassic.get(tag) != null) {
                writeMifareClassicTag(tag, data);
            } else {
                writeNdefTag(tag, ed);
            }

            Toast.makeText(getActivity(), "NFC Write Success", Toast.LENGTH_SHORT).show();

        }
    }

    private void writeMifareClassicTag(Tag tag, byte[] data) {
        MifareClassic mifareClassic = MifareClassic.get(tag);
        try {
            mifareClassic.connect();
            if (mifareClassic.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT)) {
                mifareClassic.writeBlock(1, data);
                Toast.makeText(getActivity(), "Mifare Classic tag written successfully", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Failed to authenticate Mifare Classic tag", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException while writing Mifare Classic tag", e);
        } finally {
            try {
                mifareClassic.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException while closing Mifare Classic tag", e);
            }
        }
    }

    private void writeNdefTag(Tag tag, String text) {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                ndef.writeNdefMessage(message);
                Toast.makeText(getActivity(), "NDEF tag written successfully", Toast.LENGTH_LONG).show();
                ndef.close();
            } else {
                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null) {
                    formatable.connect();
                    formatable.format(message);
                    Toast.makeText(getActivity(), "NDEF tag written successfully", Toast.LENGTH_LONG).show();
                    formatable.close();
                } else {
                    Toast.makeText(getActivity(), "Tag is not NDEF formatable", Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException | FormatException e) {
            Log.e(TAG, "IOException or FormatException while writing NDEF tag", e);
            Toast.makeText(getActivity(), "Failed to write NDEF tag", Toast.LENGTH_LONG).show();
        }
    }

    private NdefRecord createRecord(String text) {
        byte[] languageCode = Locale.getDefault().getLanguage().getBytes(Charset.forName("US-ASCII"));
        byte[] textBytes = text.getBytes(Charset.forName("UTF-8"));
        int languageCodeLength = languageCode.length;
        int textLength = textBytes.length;

        ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageCodeLength + textLength);
        payload.write((byte) (languageCodeLength & 0x1F));
        payload.write(languageCode, 0, languageCodeLength);
        payload.write(textBytes, 0, textLength);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());
    }
}
