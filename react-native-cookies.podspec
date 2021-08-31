require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name                = "react-native-cookies"
  s.version             = package["version"]
  s.summary             = package["description"]
  s.homepage            = package["homepage"]
  s.license             = package["license"]
  s.author              = { package["author"]["name"] => package["author"]["email"] }
  s.source              = { :git => "git@github.com:react-native-community/cookies.git", :tag => "v#{s.version}" }
  s.requires_arc        = true
  s.platforms           = { :ios => "7.0", :osx => "10.2" }
  s.preserve_paths      = "*.framework"
  s.ios.source_files    = "ios/**/*.{h,m}"
  s.osx.source_files    = "macos/**/*.{h,m,mm,swift}"
  s.dependency "React-Core"
end
