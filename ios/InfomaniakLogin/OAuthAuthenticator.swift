//
//  OAuthAuthenticator.swift
//  react-native-network-client
//
//  Created by Philippe Weidmann on 21.10.22.
//

import Alamofire

class OAuthAuthenticator: Authenticator {
    public typealias Credential = ApiToken
    
    open func apply(_ credential: Credential, to urlRequest: inout URLRequest) {
        urlRequest.headers.add(.authorization(bearerToken: credential.accessToken))
    }
    
    open func refresh(_ credential: Credential, for session: Session, completion: @escaping (Result<Credential, Error>) -> Void) {
        if let token = KeychainHelper.getSavedToken(),
           token.expirationDate > credential.expirationDate {
            completion(.success(token))
            return
        }
        
        InfomaniakLogin.refreshToken(token: credential) { token, error in
            // New token has been fetched correctly
            if let token = token {
                KeychainHelper.storeToken(token)
                completion(.success(token))
            } else {
                // Couldn't refresh the token, API says it's invalid
                if let error = error as NSError?, error.domain == "invalid_grant" {
                    completion(.failure(error))
                } else {
                    // Couldn't refresh the token, keep the old token and fetch it later. Maybe because of bad network ?
                    completion(.success(credential))
                }
            }
        }
    }
    
    open func didRequest(_ urlRequest: URLRequest, with response: HTTPURLResponse, failDueToAuthenticationError error: Error) -> Bool {
        return response.statusCode == 401
    }
    
    open func isRequest(_ urlRequest: URLRequest, authenticatedWith credential: ApiToken) -> Bool {
        let bearerToken = HTTPHeader.authorization(bearerToken: credential.accessToken).value
        return urlRequest.headers["Authorization"] == bearerToken
    }
}

extension ApiToken: AuthenticationCredential {
    public var requiresRefresh: Bool {
        return Date() > expirationDate
    }
}
