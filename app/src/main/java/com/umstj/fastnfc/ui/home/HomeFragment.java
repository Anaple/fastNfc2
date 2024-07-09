package com.umstj.fastnfc.ui.home;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.umstj.fastnfc.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment implements NfcAdapter.ReaderCallback{

    private FragmentHomeBinding binding;
    private NfcAdapter mNfcAdapter;
    private TextView textView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

        if (mNfcAdapter == null) {
            Toast.makeText(getActivity(), "This device doesn't support NFC.", Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(getActivity(), "NFC is disabled.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        // Set up a listener for any NFC tag or card detected by the device
        mNfcAdapter.enableReaderMode(getActivity(), this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Disable reader mode when the app is in the background
        mNfcAdapter.disableReaderMode(getActivity());
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        // This method is called when an NFC tag or card is detected by the device

        // Get the ID of the tag
        byte[] tagId = tag.getId();

        // Read the first sector of the tag
        byte[] sector0 = null;

        try {
            sector0 = readSector(tag, 0);

        } catch (IOException e) {
            e.printStackTrace();
        }


        // Convert the sector data to a string
        String sector0Data = "0";
        if (sector0[0] != 0x00){
            sector0Data = String.valueOf(sector0[0] & 0xFF);
        }

        // Display the sector data in a text view
        String finalSector0Data = sector0Data;
        textView.setText(sector0Data);
        Log.i("REad",finalSector0Data);



    }

    private byte[] readSector(Tag tag, int sectorIndex) throws IOException {
        // Get an instance of the MifareClassic class for the tag
        MifareClassic mifare = MifareClassic.get(tag);

        // Connect to the tag
        mifare.connect();

        // Get the number of sectors in the tag
        int sectorCount = mifare.getSectorCount();

        if (sectorIndex < 0 || sectorIndex >= sectorCount) {
            throw new IllegalArgumentException("Invalid sector index.");
        }

        // Authenticate the sector with key A
        boolean auth = mifare.authenticateSectorWithKeyA(sectorIndex, MifareClassic.KEY_DEFAULT);

        if (!auth) {
            throw new IOException("Authentication failed for sector " + sectorIndex);
        }

        // Read the data blocks in the sector
        byte[] sectorData = mifare.readBlock(1);

        // Disconnect from the tag
        mifare.close();

        return sectorData;
    }

    private static String ByteArrayToHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; ++i) {
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        return sb.toString();
    }

    private void writeBlockToSectorZero(byte[] data ,Tag tag) {
        try {
            MifareClassic mifare = MifareClassic.get(tag);
            mifare.connect();

            // Authenticate with the default key
            boolean auth = mifare.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT);
            if (!auth) {
                // Authentication failed
                return;
            }

            // Write the data to block 0 in sector 0
            mifare.writeBlock(0, data);

            mifare.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}