/*
 Infomaniak Mail - iOS App
 Copyright (C) 2022 Infomaniak Network SA

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import Foundation

@objc(KeychainHelper)
public class KeychainHelper: NSObject {
    private static let accessGroup = "group.com.infomaniak.chat"
    private static let keychainQueue = DispatchQueue(label: "com.infomaniak.keychain")

    private static let lockedKey = "isLockedKey"
    private static let lockedValue = "locked".data(using: .utf8)!
    private static var accessiblityValueWritten = false

    private static let apiTokenKey = "apiToken"

    static var isKeychainAccessible: Bool {
        if !accessiblityValueWritten {
            initKeychainAccessibility()
        }

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: KeychainHelper.lockedKey,
            kSecAttrAccessGroup as String: accessGroup,
            kSecReturnData as String: kCFBooleanTrue as Any,
            kSecReturnAttributes as String: kCFBooleanTrue as Any,
            kSecReturnRef as String: kCFBooleanTrue as Any,
            kSecMatchLimit as String: kSecMatchLimitAll
        ]

        var result: AnyObject?

        let resultCode = withUnsafeMutablePointer(to: &result) {
            SecItemCopyMatching(query as CFDictionary, UnsafeMutablePointer($0))
        }

        if resultCode == noErr, let array = result as? [[String: Any]] {
            for item in array {
                if let value = item[kSecValueData as String] as? Data {
                    return value == KeychainHelper.lockedValue
                }
            }
            return false
        } else {
            print("[Keychain] Accessible error ? \(resultCode == noErr), \(resultCode)")
            return false
        }
    }

    private static func initKeychainAccessibility() {
        accessiblityValueWritten = true
        let queryAdd: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccessGroup as String: accessGroup,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock,
            kSecAttrService as String: KeychainHelper.lockedKey,
            kSecValueData as String: KeychainHelper.lockedValue
        ]
        let resultCode = SecItemAdd(queryAdd as CFDictionary, nil)
        print(
            "[Keychain] Successfully init KeychainHelper ? \(resultCode == noErr || resultCode == errSecDuplicateItem), \(resultCode)"
        )
    }

    public static func deleteToken() {
        keychainQueue.sync {
            let queryDelete: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: apiTokenKey,
                kSecAttrAccount as String: apiTokenKey
            ]
            let resultCode = SecItemDelete(queryDelete as CFDictionary)
            print("Successfully deleted token ? \(resultCode == noErr)")
        }
    }

    public static func storeToken(_ token: ApiToken) {
        var resultCode: OSStatus = noErr
        // swiftlint:disable force_try
        let tokenData = try! JSONEncoder().encode(token)

        if let savedToken = getSavedToken() {
            keychainQueue.sync {
                // Save token only if it's more recent
                if savedToken.expirationDate < token.expirationDate {
                    let queryUpdate: [String: Any] = [
                        kSecClass as String: kSecClassGenericPassword,
                        kSecAttrService as String: apiTokenKey,
                        kSecAttrAccount as String: apiTokenKey
                    ]

                    let attributes: [String: Any] = [
                        kSecValueData as String: tokenData
                    ]
                    resultCode = SecItemUpdate(queryUpdate as CFDictionary, attributes as CFDictionary)
                    print("Successfully updated token ? \(resultCode == noErr)")
                }
            }
        } else {
            deleteToken()
            keychainQueue.sync {
                let queryAdd: [String: Any] = [
                    kSecClass as String: kSecClassGenericPassword,
                    kSecAttrService as String: apiTokenKey,
                    kSecAttrAccount as String: apiTokenKey,
                    kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock,
                    kSecValueData as String: tokenData
                ]
                resultCode = SecItemAdd(queryAdd as CFDictionary, nil)
                print("Successfully saved token ? \(resultCode == noErr)")
            }
        }
    }

    public static func getSavedToken() -> ApiToken? {
        var savedToken: ApiToken?
        keychainQueue.sync {
            let queryFindOne: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: apiTokenKey,
                kSecAttrAccount as String: apiTokenKey,
                kSecReturnData as String: kCFBooleanTrue as Any,
                kSecReturnAttributes as String: kCFBooleanTrue as Any,
                kSecReturnRef as String: kCFBooleanTrue as Any,
                kSecMatchLimit as String: kSecMatchLimitOne
            ]
            var result: AnyObject?

            let resultCode = withUnsafeMutablePointer(to: &result) {
                SecItemCopyMatching(queryFindOne as CFDictionary, UnsafeMutablePointer($0))
            }

            let jsonDecoder = JSONDecoder()
            if resultCode == noErr,
               let keychainItem = result as? [String: Any],
               let value = keychainItem[kSecValueData as String] as? Data,
               let token = try? jsonDecoder.decode(ApiToken.self, from: value) {
                savedToken = token
            }
        }
        return savedToken
    }
}
