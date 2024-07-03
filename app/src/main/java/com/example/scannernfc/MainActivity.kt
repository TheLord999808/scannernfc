package com.example.scannernfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.UnsupportedEncodingException
import java.util.Arrays

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    // Déclaration des variables nécessaires pour l'utilisation du NFC
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null
    private lateinit var nfcStatusTextView: TextView
    private lateinit var scanButton: Button

    // Méthode appelée lors de la création de l'activité
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des vues
        nfcStatusTextView = findViewById(R.id.nfc_status)
        scanButton = findViewById(R.id.button_scan)

        // Initialisation de l'adaptateur NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Vérification si le NFC est disponible sur l'appareil
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_LONG).show()
            finish() // Ferme l'application si le NFC n'est pas disponible
            return
        }

        // Configuration de l'intention en attente pour le NFC
        pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE
        )

        // Configuration du filtre d'intention pour le type de données NDEF
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*") // Accepte toutes les données MIME
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("Failed to add MIME type.", e)
            }
        }

        // Initialisation des filtres d'intention et des technologies NFC acceptées
        intentFiltersArray = arrayOf(ndef)
        techListsArray = arrayOf(arrayOf(android.nfc.tech.NfcF::class.java.name))

        // Définition du listener pour le bouton de scan NFC
        scanButton.setOnClickListener {
            // Code pour gérer le clic sur le bouton de scan
            Toast.makeText(this, "Scan button clicked", Toast.LENGTH_SHORT).show()
        }
    }

    // Méthode appelée lorsque l'activité devient visible pour l'utilisateur
    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    // Méthode appelée lorsque l'activité n'est plus visible pour l'utilisateur
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    // Méthode appelée lorsque l'activité reçoit une nouvelle intention
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Vérifie si l'intention reçue est de type NDEF DISCOVERED
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            // Récupère le tag NFC à partir de l'intention
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                // Obtient l'objet Ndef pour le tag
                val ndef = Ndef.get(tag)
                ndef?.let {
                    // Récupère le message NDEF du tag
                    val ndefMessage = it.cachedNdefMessage
                    ndefMessage?.let {
                        // Parcourt les enregistrements du message NDEF
                        val records = it.records
                        for (record in records) {
                            // Vérifie si l'enregistrement est de type texte bien connu
                            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_TEXT)) {
                                try {
                                    // Récupère le texte de la charge utile de l'enregistrement
                                    val payload = record.payload
                                    val text = String(payload, 1, payload.size - 1, charset("UTF-8"))
                                    // Affiche le contenu du tag NFC dans le TextView
                                    nfcStatusTextView.text = "NFC Tag Content: $text"
                                } catch (e: UnsupportedEncodingException) {
                                    Log.e("NFC", "Unsupported Encoding", e)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}