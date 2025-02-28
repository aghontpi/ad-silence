#!/usr/bin/env python3

import sys
import zipfile
# pip install asn1crypto
from asn1crypto import cms, x509

def extract_certificates_from_apk(apk_path):
    """
    Extract all certificates from an APK by reading signature blocks (.RSA/.DSA/.EC)
    in the META-INF directory. Returns a list of asn1crypto.x509.Certificate objects.
    """
    certs = []
    with zipfile.ZipFile(apk_path, 'r') as z:
        # Find possible signature files in META-INF
        signature_files = [
            name for name in z.namelist()
            if name.upper().startswith("META-INF/")
            and (name.upper().endswith(".RSA")
                 or name.upper().endswith(".DSA")
                 or name.upper().endswith(".EC"))
        ]

        for sig_file in signature_files:
            data = z.read(sig_file)
            # Parse the file as a CMS (PKCS#7) ContentInfo structure
            try:
                content_info = cms.ContentInfo.load(data)
                if content_info['content_type'].native == 'signed_data':
                    signed_data = content_info['content']
                    # The 'certificates' field can contain multiple X.509 certs
                    for c in signed_data['certificates']:
                        if isinstance(c.chosen, x509.Certificate):
                            certs.append(c.chosen)
            except Exception as e:
                # Not every file with .RSA/.DSA/.EC necessarily is parseable
                # or may not be a PKCS#7 container. Ignore or log as needed.
                print(f"Warning: Could not parse {sig_file} - {e}")

    return certs

def certificate_info_str(cert):
    """
    Build a string with relevant certificate information, including
    Subject, Issuer, and Serial Number.
    """
    subject = cert.subject.native
    issuer = cert.issuer.native
    serial = cert.serial_number

    # You can format subject/issuer as you like. This is just an example:
    info = (
        f"Subject: {subject}\n"
        f"Issuer:  {issuer}\n"
        f"Serial:  {serial}\n"
    )
    return info

def compare_certificates(certs1, certs2):
    """
    Compare two lists of certificates.
    Returns True if there's at least one matching certificate in both lists.

    You can refine the comparison logic as needed, for example:
      - Compare entire DER-encoded certs
      - Compare subject/issuer/serial only
      - Compare public key or fingerprint, etc.
    """
    # For a strict check, compare exact DER bytes (which ensures same certificate).
    # Another option is to check subject, issuer, serial, etc. For demonstration:
    der1 = [c.dump() for c in certs1]
    der2 = [c.dump() for c in certs2]

    # If any certificate's DER in set1 is found in set2, we treat them as "the same".
    set1 = set(der1)
    set2 = set(der2)
    return not set1.isdisjoint(set2)

def main(apk1, apk2):
    # Extract the certificates
    certs1 = extract_certificates_from_apk(apk1)
    certs2 = extract_certificates_from_apk(apk2)

    # If no certificates found, warn and exit
    if not certs1:
        print(f"No certificates extracted from: {apk1}")
    else:
        print(f"Certificates extracted from: {apk1}\n")
        for i, c in enumerate(certs1, 1):
            print(f"--- Certificate {i} ---")
            print(certificate_info_str(c))

    print("========================================\n")

    if not certs2:
        print(f"No certificates extracted from: {apk2}")
    else:
        print(f"Certificates extracted from: {apk2}\n")
        for i, c in enumerate(certs2, 1):
            print(f"--- Certificate {i} ---")
            print(certificate_info_str(c))

    print("========================================\n")

    # Compare them
    if compare_certificates(certs1, certs2):
        print("Result: At least one matching certificate found.")
    else:
        print("Result: No matching certificates found.")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(f"Usage: python {sys.argv[0]} <apk1> <apk2>")
        sys.exit(1)

    apk1_path = sys.argv[1]
    apk2_path = sys.argv[2]
    main(apk1_path, apk2_path)
